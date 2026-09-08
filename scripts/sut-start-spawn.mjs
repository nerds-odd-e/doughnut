import { writeFile } from 'node:fs/promises'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { LOG_TARGETS } from './log-utils.mjs'
import {
  resolveSutRuntimeTarget,
  withSutRuntimeTargetEnv,
} from './sut-runtime-target.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

export const LOG_FILE = LOG_TARGETS.sut
export const PID_FILE = path.join(repoRoot, 'sut.pid')

/**
 * Spawn the SUT service group detached. The wrapper keeps running after this
 * startup helper exits and writes stdout+stderr through a rotating log.
 *
 * @param {{ spawnFn?: typeof spawn, logFile?: string, runtimeTarget?: object }} [opts]
 * @returns {{ child: import('node:child_process').ChildProcess, logFile: string }}
 */
export function spawnSutServices({
  spawnFn = spawn,
  logFile = LOG_FILE,
  runtimeTarget,
  owner,
  checkoutRoot = repoRoot,
} = {}) {
  const target = resolveSutRuntimeTarget({ runtimeTarget })
  const ownerEnv = {
    SUT_CHECKOUT_ROOT: checkoutRoot,
    ...(owner
      ? {
          SUT_OWNER_TOKEN: owner.token,
          SUT_OWNER_CONTROL_PATH: owner.controlPath,
        }
      : {}),
  }
  const child = spawnFn(
    process.execPath,
    [path.join(repoRoot, 'scripts/sut-services.mjs')],
    {
      cwd: repoRoot,
      detached: true,
      env: withSutRuntimeTargetEnv(
        { ...process.env, SUT_LOG_FILE: logFile, ...ownerEnv },
        target
      ),
      stdio: 'ignore',
      shell: false,
    }
  )
  child.unref()
  return { child, logFile }
}

/**
 * Write the process group id (negative of PID) to the PID file so external
 * tools can kill the whole group with `process.kill(-pgid, 'SIGTERM')`.
 *
 * @param {number} pid
 * @param {{ pidFile?: string }} [opts]
 */
export async function writePidFile(pid, { pidFile = PID_FILE } = {}) {
  await writeFile(pidFile, String(pid), 'utf8')
}
