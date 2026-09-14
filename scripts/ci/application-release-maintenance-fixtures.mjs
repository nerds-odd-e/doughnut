import { spawnSync } from 'node:child_process'
import {
  mkdtempSync,
  mkdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const repositoryRoot = fileURLToPath(new URL('../../', import.meta.url))
const scriptPath = join(
  repositoryRoot,
  'infra/gcp/scripts/enter-maintenance-mode.sh'
)

// Builds a fake-cloud-command fixture for enter-maintenance-mode.sh,
// following the same trace-recording pattern as
// application-release-publication-fixtures.mjs's makePublication, but
// without that fixture's git-checkout/publication apparatus -- this script
// never reads the release repository.
//
// scenario:
//   'quiesces-after-polling' -- list-instances reports RUNNING for the
//     first two checks, then TERMINATED; the script must poll before
//     succeeding.
//   'quiescence-never-verified' -- list-instances always reports RUNNING;
//     the timeout is set to 0 so the script fails on its first check.
export function makeMaintenanceEntry(t, scenario = 'quiesces-after-polling') {
  const root = mkdtempSync(join(tmpdir(), 'application-release-maintenance-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const bin = join(root, 'bin')
  mkdirSync(bin)
  const trace = join(root, 'trace')
  const callCountFile = join(root, 'list-instances-call-count')

  const quiesceAfterCalls = scenario === 'quiesces-after-polling' ? 3 : 999999

  writeFileSync(
    join(bin, 'gcloud'),
    `#!/usr/bin/env bash
set -euo pipefail
echo "gcloud $*" >> "$TRACE"
if [[ "$*" == *"stop-instances"* ]]; then
  exit 0
fi
if [[ "$*" == *"list-instances"* ]]; then
  count=$(cat "$CALL_COUNT_FILE" 2>/dev/null || echo 0)
  count=$((count + 1))
  echo "$count" > "$CALL_COUNT_FILE"
  if (( count < QUIESCE_AFTER_CALLS )); then
    echo RUNNING
  else
    echo TERMINATED
  fi
  exit 0
fi
echo "unexpected gcloud invocation: $*" >&2
exit 1
`,
    { mode: 0o755 }
  )

  const enter = () =>
    spawnSync('bash', [scriptPath], {
      cwd: root,
      encoding: 'utf8',
      env: {
        ...process.env,
        PATH: `${bin}:${process.env.PATH}`,
        TRACE: trace,
        CALL_COUNT_FILE: callCountFile,
        QUIESCE_AFTER_CALLS: String(quiesceAfterCalls),
        ZONE: 'us-east1-b',
        MIG_NAME: 'doughnut-app-group',
        MAINTENANCE_STOP_GRACE_SECONDS: '0',
        MAINTENANCE_QUIESCENCE_POLL_SECONDS: '0',
        MAINTENANCE_QUIESCENCE_TIMEOUT_SECONDS:
          scenario === 'quiescence-never-verified' ? '0' : '300',
      },
    })

  const traceCalls = () =>
    readFileSync(trace, 'utf8').trim().split('\n').filter(Boolean)

  return { enter, root, trace, traceCalls }
}
