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

export const OLD_TEMPLATE_NAME =
  'doughnut-app-debian12-zulu25-openai-mig-template-old'
export const COMPATIBLE_TEMPLATE_NAME =
  'doughnut-app-debian12-zulu25-openai-mig-template-portable-trash'

// Builds a fake-cloud-command fixture for enter-maintenance-mode.sh,
// following the same trace-recording pattern as
// application-release-publication-fixtures.mjs's makePublication, but
// without that fixture's git-checkout/publication apparatus -- this script
// never reads the release repository.
//
// The fake gcloud also persists the MIG's `instanceTemplate` field to a
// state file, mirroring the one piece of durable, cloud-owned state this
// slice relies on: `set-instance-template` writes it, and a `describe`
// invocation (exposed as `describeCurrentTemplate`, called independently of
// `enter`) reads it back -- standing in for what GCP's own autohealing or
// proactive-replace control loop, or an operator's retry, would observe on
// a later, separate invocation after this script's process is gone.
//
// scenario:
//   'quiesces-after-polling' -- list-instances reports RUNNING for the
//     first two checks, then TERMINATED; the script must poll before
//     succeeding.
//   'quiescence-never-verified' -- list-instances always reports RUNNING;
//     the timeout is set to 0 so the script fails on its first check.
//   'interrupted-after-template-set' -- the instance-template assignment
//     succeeds (and durably commits), but the subsequent stop-instances
//     call fails, modeling a process/connection loss between the two
//     steps. Migration must not be permitted, but the compatible template
//     must already be the durable MIG state.
export function makeMaintenanceEntry(t, scenario = 'quiesces-after-polling') {
  const root = mkdtempSync(join(tmpdir(), 'application-release-maintenance-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const bin = join(root, 'bin')
  mkdirSync(bin)
  const trace = join(root, 'trace')
  const callCountFile = join(root, 'list-instances-call-count')
  const templateStateFile = join(root, 'mig-instance-template')
  writeFileSync(templateStateFile, OLD_TEMPLATE_NAME)
  writeFileSync(trace, '')

  const quiesceAfterCalls = scenario === 'quiesces-after-polling' ? 3 : 999999
  const stopInstancesShouldFail = scenario === 'interrupted-after-template-set'

  writeFileSync(
    join(bin, 'gcloud'),
    `#!/usr/bin/env bash
set -euo pipefail
echo "gcloud $*" >> "$TRACE"
for arg in "$@"; do
  case "$arg" in
    --template=*) echo "\${arg#--template=}" > "$TEMPLATE_STATE_FILE" ;;
  esac
done
if [[ "$*" == *"set-instance-template"* ]]; then
  exit 0
fi
if [[ "$*" == *"describe"* && "$*" == *"instanceTemplate"* ]]; then
  cat "$TEMPLATE_STATE_FILE"
  exit 0
fi
if [[ "$*" == *"stop-instances"* ]]; then
  if [[ "\${STOP_INSTANCES_SHOULD_FAIL:-}" == "1" ]]; then
    exit 1
  fi
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

  const baseEnv = {
    ...process.env,
    PATH: `${bin}:${process.env.PATH}`,
    TRACE: trace,
    TEMPLATE_STATE_FILE: templateStateFile,
    CALL_COUNT_FILE: callCountFile,
    QUIESCE_AFTER_CALLS: String(quiesceAfterCalls),
    STOP_INSTANCES_SHOULD_FAIL: stopInstancesShouldFail ? '1' : '0',
    ZONE: 'us-east1-b',
    MIG_NAME: 'doughnut-app-group',
    MAINTENANCE_INSTANCE_TEMPLATE: COMPATIBLE_TEMPLATE_NAME,
    MAINTENANCE_STOP_GRACE_SECONDS: '0',
    MAINTENANCE_QUIESCENCE_POLL_SECONDS: '0',
    MAINTENANCE_QUIESCENCE_TIMEOUT_SECONDS:
      scenario === 'quiescence-never-verified' ? '0' : '300',
  }

  const enter = (envOverrides = {}) =>
    spawnSync('bash', [scriptPath], {
      cwd: root,
      encoding: 'utf8',
      env: { ...baseEnv, ...envOverrides },
    })

  const traceCalls = () =>
    readFileSync(trace, 'utf8').trim().split('\n').filter(Boolean)

  // Simulates a later, independent gcloud invocation -- the kind
  // autohealing, the update policy's proactive convergence, or an
  // operator's retry would make -- reading the MIG's durable
  // instanceTemplate state after enter-maintenance-mode.sh's own process
  // is gone.
  const describeCurrentTemplate = () =>
    spawnSync(
      join(bin, 'gcloud'),
      [
        'compute',
        'instance-groups',
        'managed',
        'describe',
        'doughnut-app-group',
        '--zone=us-east1-b',
        '--format=value(instanceTemplate)',
      ],
      {
        encoding: 'utf8',
        env: { ...baseEnv },
      }
    ).stdout.trim()

  return { enter, root, trace, traceCalls, describeCurrentTemplate }
}
