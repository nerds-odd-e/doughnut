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

export function makeMaintenanceEntry(t, scenario = 'closes') {
  const root = mkdtempSync(join(tmpdir(), 'application-release-maintenance-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const bin = join(root, 'bin')
  mkdirSync(bin)
  const trace = join(root, 'trace')
  const targetSizeStateFile = join(root, 'target-size')
  const updatePolicyStateFile = join(root, 'update-policy')
  writeFileSync(trace, '')
  writeFileSync(targetSizeStateFile, '2')
  writeFileSync(updatePolicyStateFile, 'PROACTIVE')

  writeFileSync(
    join(bin, 'gcloud'),
    `#!/usr/bin/env bash
set -euo pipefail
echo "gcloud $*" >> "$TRACE"
if [[ "$*" == *" managed describe "* ]]; then
  echo 2
elif [[ "$*" == *" managed update "* ]]; then
  echo OPPORTUNISTIC > "$UPDATE_POLICY_STATE_FILE"
elif [[ "$*" == *" managed resize "* ]]; then
  echo 0 > "$TARGET_SIZE_STATE_FILE"
elif [[ "$*" == *" managed list-instances "* ]]; then
  [[ "\${LEAVE_INSTANCE:-}" == 1 ]] && echo zones/us-east1-b/instances/old-instance
else
  echo "unexpected gcloud invocation: $*" >&2
  exit 1
fi
if [[ "\${FAIL_AFTER:-}" != "" && "$*" == *" managed \${FAIL_AFTER} "* ]]; then exit 86; fi
`,
    { mode: 0o755 }
  )

  const baseEnv = {
    ...process.env,
    PATH: `${bin}:${process.env.PATH}`,
    TRACE: trace,
    TARGET_SIZE_STATE_FILE: targetSizeStateFile,
    UPDATE_POLICY_STATE_FILE: updatePolicyStateFile,
    LEAVE_INSTANCE: scenario === 'never-closes' ? '1' : '',
    FAIL_AFTER: scenario.startsWith('fails-after-') ? scenario.slice(12) : '',
    MAINTENANCE_STOP_GRACE_SECONDS: '0',
    MAINTENANCE_QUIESCENCE_POLL_SECONDS: '0',
    MAINTENANCE_QUIESCENCE_TIMEOUT_SECONDS:
      scenario === 'never-closes' ? '0' : '5',
  }
  const enter = () =>
    spawnSync('bash', [scriptPath], {
      cwd: root,
      encoding: 'utf8',
      env: baseEnv,
    })
  const traceCalls = () =>
    readFileSync(trace, 'utf8').trim().split('\n').filter(Boolean)
  const state = () => ({
    targetSize: readFileSync(targetSizeStateFile, 'utf8').trim(),
    updatePolicy: readFileSync(updatePolicyStateFile, 'utf8').trim(),
  })
  return { enter, traceCalls, state }
}
