import { execFileSync, spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import {
  cpSync,
  mkdirSync,
  readFileSync,
  realpathSync,
  symlinkSync,
  writeFileSync,
} from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { parse } from 'yaml'
import { makeReleaseRepository } from './application-release-fixtures.mjs'

const repositoryRoot = fileURLToPath(new URL('../../', import.meta.url))
const publicationCommand = parse(
  readFileSync(join(repositoryRoot, '.github/workflows/deploy.yml'), 'utf8')
).jobs.Deploy.steps.find((step) => step.id === 'publish').run
export const hash = (content) =>
  createHash('sha256').update(content).digest('hex')
export const readApplicationRecords = (path) =>
  readFileSync(path, 'utf8')
    .trim()
    .split('\n')
    .map((record) => JSON.parse(record))

export function makePublication(t, scenario = 'skip') {
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
    join(origin, 'infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh'),
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
  symlinkSync(join(repositoryRoot, 'node_modules'), join(root, 'node_modules'))
  const bin = join(root, 'bin')
  mkdirSync(bin)
  const artifacts = (runId) => ({
    frontend: join(root, `artifacts-${runId}/frontend`),
    cli: join(root, `artifacts-${runId}/bundle.mjs`),
    jar: join(root, `artifacts-${runId}/donut.jar`),
  })
  for (const runId of [42, 99]) {
    const payload = artifacts(runId)
    mkdirSync(payload.frontend, { recursive: true })
    const label = runId === 42 ? 'selected' : 'fresh'
    writeFileSync(join(payload.frontend, 'index.html'), `${label} SPA`)
    writeFileSync(payload.cli, `${label} CLI`)
    writeFileSync(payload.jar, `${label} jar`)
  }
  const { frontend, cli, jar } = artifacts(42)
  const backendRecord = join(root, 'record.json')
  writeFileSync(
    backendRecord,
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
if [[ "$1" == -m ]]; then cp "$4/index.html" "$CAPTURED_SPA"; fi
if [[ "$1" == cp && "$2" == -a ]]; then cp "$4" "$CAPTURED_CLI"; fi
if [[ -n "\${FAIL_GSUTIL_MATCH:-}" && "$*" == *"$FAIL_GSUTIL_MATCH"* ]]; then exit 37; fi
if [[ "$1" == cp && "$2" == - && "$3" == */application-release.json ]]; then
  cat >> "$APP_RECORDS"
fi
if [[ "$1" == cp && "$2" == - && "$3" == */last-successful-deploy.json ]]; then
  cat > "$SAVED_RECORD"
fi`
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
  const publish = (
    release = { sha, ref: 'refs/tags/v1.2.3', refOid },
    ci = { runId: 42, runAttempt: 3 },
    { failGsutilMatch = '' } = {}
  ) =>
    spawnSync('bash', ['-c', publicationCommand], {
      cwd: repositoryRoot,
      encoding: 'utf8',
      env: {
        ...process.env,
        PATH: `${bin}:${process.env.PATH}`,
        TRACE: trace,
        RECORD: backendRecord,
        APP_RECORDS: join(root, 'application-records'),
        SAVED_RECORD: join(root, 'saved-record'),
        CAPTURED_SPA: join(root, 'captured-spa'),
        CAPTURED_CLI: join(root, 'captured-cli'),
        GITHUB_SHA: 'f'.repeat(40),
        RELEASE_SHA: release.sha,
        RELEASE_SOURCE_ROOT: root,
        RELEASE_REF: release.ref,
        RELEASE_REF_OID: release.refOid,
        RELEASE_CI_RUN_ID: String(ci.runId),
        RELEASE_CI_RUN_ATTEMPT: String(ci.runAttempt),
        CAPTURED_MAP: join(root, 'captured-map'),
        CAPTURED_STARTUP: join(root, 'captured-startup'),
        GCS_BUCKET: 'private-backend',
        GCS_FRONTEND_BUCKET: 'public-frontend',
        ARTIFACT: 'donut',
        VERSION: '0.0.1-SNAPSHOT',
        FRONTEND_STATIC_DIR: artifacts(ci.runId).frontend,
        CLI_BUNDLE_SOURCE: artifacts(ci.runId).cli,
        DEPLOY_JAR_PATH: artifacts(ci.runId).jar,
        FORCE_FULL_DEPLOY: '',
        HEALTHCHECK_RETRY_SLEEP_SECONDS: '0',
        FAIL_GSUTIL_MATCH:
          failGsutilMatch || (scenario === 'failed-frontend' ? 'rsync' : ''),
      },
    })
  return {
    fixture,
    publish,
    root,
    sha,
    refOid,
    trace,
    applicationRecords: join(root, 'application-records'),
    backendRecord,
    frontend,
    cli,
    jar,
    startup,
  }
}
