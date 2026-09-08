#!/usr/bin/env node
/**
 * Start SUT services in the background, wait for health, then exit.
 *
 * Exit 0: all services healthy.
 * Exit 1: timeout or early process exit — diagnostics on stderr, log path printed.
 *
 * Topology reference: docs/gcp/prod_env.md (Local dev / Cypress).
 * Log file: sut.log (repo root, gitignored).
 * PID file: sut.pid (repo root, gitignored) — stores the process group ID so
 *           `pnpm sut:restart` can find and stop the group.
 *
 * Env:
 *   SUT_TIMEOUT_MS  – max ms to wait for healthy (default: 120000)
 *   SUT_POLL_MS     – ms between healthcheck polls (default: 3000)
 */
import { writeFile } from 'node:fs/promises'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { refuseUnsupportedIsolatedBrowserCommand } from './browser-worktree-isolation.mjs'
import { LOG_TARGETS } from './log-utils.mjs'
import { waitForSutHealthy } from './sut-start-health-wait.mjs'
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
} = {}) {
  const target = resolveSutRuntimeTarget({ runtimeTarget })
  const child = spawnFn(
    process.execPath,
    [path.join(repoRoot, 'scripts/sut-services.mjs')],
    {
      cwd: repoRoot,
      detached: true,
      env: withSutRuntimeTargetEnv(
        { ...process.env, SUT_LOG_FILE: logFile },
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

/**
 * Full start flow: spawn services, write PID file, wait for health.
 *
 * @param {{
 *   spawnFn?: typeof spawn,
 *   logFile?: string,
 *   pidFile?: string,
 *   timeoutMs?: number,
 *   pollMs?: number,
 *   log?: (s: string) => void,
 *   errLog?: (s: string) => void,
 *   healthcheckFn?: Function,
 *   checkoutRoot?: string,
 *   runtimeTarget?: object,
 * }} [opts]
 * @returns {Promise<number>} exit code (0 = healthy, 1 = failed)
 */
export async function runSutStart({
  spawnFn = spawn,
  logFile = LOG_FILE,
  pidFile = PID_FILE,
  timeoutMs,
  pollMs,
  log = (s) => process.stdout.write(`${s}\n`),
  errLog = (s) => process.stderr.write(`${s}\n`),
  healthcheckFn,
  checkoutRoot = repoRoot,
  runtimeTarget,
} = {}) {
  refuseUnsupportedIsolatedBrowserCommand({
    checkoutRoot,
    command: 'pnpm sut',
  })
  const target = resolveSutRuntimeTarget({ runtimeTarget })
  log(`Starting SUT services... (log: ${logFile})`)
  const { child } = spawnSutServices({
    spawnFn,
    logFile,
    runtimeTarget: target,
  })
  await writePidFile(child.pid, { pidFile })

  const { exitCode } = await waitForSutHealthy({
    child,
    timeoutMs,
    pollMs,
    logFile,
    log,
    errLog,
    healthcheckFn,
    runtimeTarget: target,
  })
  return exitCode
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    const code = await runSutStart()
    process.exit(code)
  } catch (e) {
    process.stderr.write(`${e instanceof Error ? e.message : String(e)}\n`)
    process.exit(1)
  }
}
