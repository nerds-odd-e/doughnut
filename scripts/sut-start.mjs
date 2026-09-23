/** Legacy detached-start adapter and public SUT lifetime exports. */
import path from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { LOG_FILE, PID_FILE } from './sut-start-spawn.mjs'
import { startOwnedSutLifetime } from './sut-owned-lifetime.mjs'

export { startOwnedSutLifetime }
export {
  LOG_FILE,
  PID_FILE,
  spawnSutServices,
  writePidFile,
} from './sut-start-spawn.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

/**
 * Legacy start adapter: spawn services, write PID file, wait for health, and
 * return an exit code. On a healthy start the detached supervisor keeps
 * running. On a failed isolated start the owned tree and ownership are
 * released.
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
 *   databaseExistsFn?: (database: string) => boolean,
 *   mysqlExecFn?: typeof import('node:child_process').execFileSync,
 *   schemaExistsFn?: (database: string) => boolean,
 *   isPortOccupiedFn?: (port: number) => Promise<boolean>,
 *   portClaimRoot?: string,
 *   retainOwnership?: boolean,
 *   signal?: AbortSignal,
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
  databaseExistsFn,
  mysqlExecFn,
  schemaExistsFn,
  isPortOccupiedFn,
  portClaimRoot,
  retainOwnership = false,
  signal,
} = {}) {
  const lifetime = await startOwnedSutLifetime({
    spawnFn,
    logFile,
    pidFile,
    timeoutMs,
    pollMs,
    log,
    errLog,
    healthcheckFn,
    checkoutRoot,
    runtimeTarget,
    databaseExistsFn,
    mysqlExecFn,
    schemaExistsFn,
    isPortOccupiedFn,
    portClaimRoot,
    retainOwnership,
    signal,
  })
  const { exitCode } = await lifetime.ready
  if (exitCode !== 0) await lifetime.shutdown()
  return exitCode
}
