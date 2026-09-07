import assert from 'node:assert/strict'
import { execFileSync, spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import {
  cpSync,
  existsSync,
  mkdirSync,
  readFileSync,
  realpathSync,
  symlinkSync,
  writeFileSync,
} from 'node:fs'
import { dirname, join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { parse } from 'yaml'
import { makeReleaseRepository } from './application-release-fixtures.mjs'

const repositoryRoot = fileURLToPath(new URL('../../', import.meta.url))
const publicationCommand = parse(
  readFileSync(join(repositoryRoot, '.github/workflows/deploy.yml'), 'utf8')
).jobs.Deploy.steps.find((step) => step.id === 'publish').run
const hash = (content) => createHash('sha256').update(content).digest('hex')

for (const scenario of [
  'skip',
  'forced',
  'moved',
  'invalid-routing',
  'wrong-source',
]) {
  test(`selected-source publication: ${scenario}`, (t) => {
    const fixture = makeReleaseRepository(t)
    const forced = scenario === 'forced'
    const origin = join(dirname(fixture.repository), 'origin')
    for (const path of [
      'infra/gcp/path-routing',
      'infra/gcp/url-maps',
      'scripts/validate-url-map-static-vs-backend-hints.mjs',
    ]) {
      cpSync(join(repositoryRoot, path), join(origin, path), {
        recursive: true,
      })
    }
    const startup = 'echo selected-source-startup\n'
    mkdirSync(join(origin, 'infra/gcp/scripts'))
    writeFileSync(
      join(
        origin,
        'infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh'
      ),
      startup
    )
    writeFileSync(
      join(origin, 'infra/gcp/scripts/publish-application.sh'),
      'exit 99\n'
    )
    const routingPath = join(
      origin,
      'infra/gcp/path-routing/doughnut-routing.json'
    )
    const routing = JSON.parse(readFileSync(routingPath))
    routing.backendPathHints.exactPaths.push('/selected-source-only')
    if (scenario === 'invalid-routing')
      routing.backendPathHints.exactPaths.push('/index.html')
    writeFileSync(routingPath, JSON.stringify(routing))
    fixture.git('add', '.')
    const sha = fixture.commit(
      forced ? 'Release\n\nforce-deployment: true' : 'Release'
    )
    const refOid = fixture.tag('v1.2.3', true, sha)
    fixture.commit(
      forced
        ? 'New control commit'
        : 'New control commit\n\nforce-deployment: true'
    )
    fixture.clone()
    const root = realpathSync(fixture.repository)
    execFileSync('git', ['fetch', 'origin', sha], {
      cwd: root,
      stdio: 'ignore',
    })
    if (scenario !== 'wrong-source')
      execFileSync('git', ['checkout', sha], { cwd: root, stdio: 'ignore' })
    if (scenario === 'moved') fixture.git('tag', '-f', 'v1.2.3', 'HEAD')
    symlinkSync(
      join(repositoryRoot, 'node_modules'),
      join(root, 'node_modules')
    )
    const bin = join(root, 'bin')
    const frontend = join(root, 'frontend')
    mkdirSync(bin)
    mkdirSync(frontend)
    writeFileSync(join(frontend, 'index.html'), 'selected SPA')
    const cli = join(root, 'bundle.mjs')
    const jar = join(root, 'donut.jar')
    writeFileSync(cli, 'selected CLI')
    writeFileSync(jar, 'selected jar')
    const record = join(root, 'record.json')
    writeFileSync(
      record,
      JSON.stringify({
        sha256: hash('selected jar'),
        startup_script_sha256: hash(startup),
      })
    )
    const fake = (name, body) =>
      writeFileSync(
        join(bin, name),
        `#!/usr/bin/env bash\nset -euo pipefail\n${body}\n`,
        { mode: 0o755 }
      )
    fake(
      'gsutil',
      `echo "gsutil $*" >> "$TRACE"
if [[ "$1" == cat ]]; then cat "$RECORD"; fi
if [[ "$1" == cp && "$2" == - ]]; then cat > "$SAVED_RECORD"; fi`
    )
    fake(
      'gcloud',
      `echo "gcloud $*" >> "$TRACE"
for arg in "$@"; do
  if [[ "$arg" == --source=* ]]; then cp "\${arg#--source=}" "$CAPTURED_MAP"; fi
  if [[ "$arg" == startup-script=* ]]; then cp "\${arg#startup-script=}" "$CAPTURED_STARTUP"; fi
done
if [[ "$*" == *"managed describe"* ]]; then echo current-template; fi`
    )
    fake(
      'curl',
      `echo "curl $*" >> "$TRACE"
while [[ "$1" != -o ]]; do shift; done
printf 'OK . Commit: %s' "$GITHUB_SHA" > "$2"
printf 200`
    )
    const trace = join(root, 'trace')
    const result = spawnSync('bash', ['-c', publicationCommand], {
      cwd: repositoryRoot,
      encoding: 'utf8',
      env: {
        ...process.env,
        PATH: `${bin}:${process.env.PATH}`,
        TRACE: trace,
        RECORD: record,
        SAVED_RECORD: join(root, 'saved-record'),
        GITHUB_SHA: 'f'.repeat(40),
        RELEASE_SHA: sha,
        RELEASE_SOURCE_ROOT: root,
        RELEASE_REF: 'refs/tags/v1.2.3',
        RELEASE_REF_OID: refOid,
        CAPTURED_MAP: join(root, 'captured-map'),
        CAPTURED_STARTUP: join(root, 'captured-startup'),
        GCS_BUCKET: 'private-backend',
        GCS_FRONTEND_BUCKET: 'public-frontend',
        ARTIFACT: 'donut',
        VERSION: '0.0.1-SNAPSHOT',
        FRONTEND_STATIC_DIR: frontend,
        CLI_BUNDLE_SOURCE: cli,
        DEPLOY_JAR_PATH: jar,
        FORCE_FULL_DEPLOY: '',
        HEALTHCHECK_RETRY_SLEEP_SECONDS: '0',
      },
    })
    if (['moved', 'invalid-routing', 'wrong-source'].includes(scenario)) {
      assert.notEqual(result.status, 0)
      assert.equal(existsSync(trace), false, result.stdout)
      assert.match(
        result.stderr,
        scenario === 'moved'
          ? /Release ref changed/
          : scenario === 'wrong-source'
            ? /does not match selected SHA/
            : /FAILED/
      )
      return
    }
    assert.equal(result.status, 0, result.stderr)
    assert.match(
      readFileSync(join(root, 'captured-map'), 'utf8'),
      /selected-source-only/
    )
    const calls = readFileSync(trace, 'utf8').trim().split('\n')
    assert.deepEqual(calls.slice(0, 3), [
      `gsutil -m rsync -r ${frontend} gs://public-frontend/frontend/${sha}/`,
      `gsutil -h Cache-Control:public,max-age=60 cp ${frontend}/index.html gs://public-frontend/frontend/${sha}/index.html`,
      `gsutil cp -a public-read ${cli} gs://public-frontend/doughnut-cli-latest/doughnut`,
    ])
    assert.match(calls[3], /^gcloud compute url-maps import /)
    if (forced) {
      assert.ok(
        calls.includes(
          `gsutil cp ${jar} gs://private-backend/backend_app_jar/donut-0.0.1-SNAPSHOT.jar`
        )
      )
      assert.ok(calls.some((call) => call.includes('rolling-action replace')))
      assert.equal(
        JSON.parse(readFileSync(join(root, 'saved-record'))).git_sha,
        sha
      )
      assert.equal(
        readFileSync(join(root, 'captured-startup'), 'utf8'),
        startup
      )
      assert.equal(
        JSON.parse(readFileSync(join(root, 'saved-record')))
          .startup_script_sha256,
        hash(startup)
      )
    } else {
      assert.match(result.stdout, /Deploy skipped/)
      assert.equal(calls.length, 6)
    }
  })
}

test('workflows share publication commands and retain independent CLI build version', () => {
  const workflow = (name) =>
    parse(
      readFileSync(
        join(repositoryRoot, '.github/workflows', `${name}.yml`),
        'utf8'
      )
    )
  const application = workflow('deploy').jobs.Deploy.steps
  assert.ok(application.some((step) => step.id === 'publish'))
  const cli = workflow('cli-release')
  assert.deepEqual(cli.on.push.tags, ['cli-*'])
  const steps = cli.jobs['build-and-publish'].steps
  assert.ok(
    steps.some(
      (step) =>
        step.run ===
        'echo "CLI_VERSION=${GITHUB_REF#refs/tags/cli-v}" >> $GITHUB_ENV'
    )
  )
  assert.ok(steps.some((step) => step.run === 'pnpm cli:bundle'))
  assert.equal(
    steps.at(-1).run,
    'infra/gcp/scripts/upload-cli-binary-to-gcs.sh'
  )
})
