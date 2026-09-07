import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { parse } from 'yaml'
import { makeReleaseRepository } from './application-release-fixtures.mjs'

const repositoryRoot = fileURLToPath(new URL('../../', import.meta.url))
const scripts = join(repositoryRoot, 'infra/gcp/scripts')
const hash = (content) => createHash('sha256').update(content).digest('hex')

for (const forced of [false, true]) {
  test(`publication uploads frontend and CLI before ${forced ? 'forced backend rollout' : 'unchanged backend skip'}`, (t) => {
    const fixture = makeReleaseRepository(t)
    const sha = forced
      ? fixture.commit('Release\n\nforce-deployment: true')
      : fixture.sha
    fixture.clone()
    const root = fixture.repository
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
        startup_script_sha256: hash(
          readFileSync(
            join(scripts, 'mig-zulu25-openai-app-instance-startup.sh')
          )
        ),
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
    const result = spawnSync(
      'bash',
      [join(scripts, 'publish-application.sh')],
      {
        cwd: root,
        encoding: 'utf8',
        env: {
          ...process.env,
          PATH: `${bin}:${process.env.PATH}`,
          TRACE: trace,
          RECORD: record,
          SAVED_RECORD: join(root, 'saved-record'),
          GITHUB_SHA: sha,
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
      }
    )
    assert.equal(result.status, 0, result.stderr)
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
  assert.ok(
    application.some(
      (step) => step.run === 'bash infra/gcp/scripts/publish-application.sh'
    )
  )
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
