#!/usr/bin/env node
/**
 * Start SUT services in the background, wait for health, then exit.
 *
 * Exit 0: all services healthy.
 * Exit 1: timeout, cancellation, or early process exit — diagnostics on stderr, log path printed.
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
import path from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import {
  refuseUnsupportedIsolatedBrowserCommand,
  worktreeIsolationApplies,
} from './browser-worktree-isolation.mjs'
import { ensureIsolatedE2eDatabase } from './sut-e2e-database.mjs'
import { ensureIsolatedE2ePorts } from './sut-e2e-ports.mjs'
import { checkTcpPort } from './sut-healthcheck.mjs'
import {
  assertAllocatedPortsFree,
  assertE2eDatabaseExists,
  isolatedBrowserOrigin,
  refuseConflictingSutOverrides,
  resolveSutCheckoutTarget,
} from './sut-isolated-target.mjs'
import {
  assertNoLiveSutOwner,
  claimSutOwnership,
  readHeldSutOwner,
  releaseSutOwnership,
} from './sut-owner.mjs'
import { stopOwnedSutProcessTree } from './sut-owned-process-tree.mjs'
import { waitForSutHealthy } from './sut-start-health-wait.mjs'
import {
  LOG_FILE,
  PID_FILE,
  spawnSutServices,
  writePidFile,
} from './sut-start-spawn.mjs'

export { LOG_FILE, PID_FILE, spawnSutServices, writePidFile }

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

async function releaseFailedIsolatedStart({ child, checkoutRoot }) {
  await stopOwnedSutProcessTree(child)
  await releaseSutOwnership(checkoutRoot)
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
 *   databaseExistsFn?: (database: string) => boolean,
 *   mysqlExecFn?: typeof import('node:child_process').execFileSync,
 *   schemaExistsFn?: (database: string) => boolean,
 *   isPortOccupiedFn?: (port: number) => Promise<boolean>,
 *   portClaimRoot?: string,
 *   retainOwnership?: boolean,
 *   signal?: AbortSignal,
 *   attachCancelSignals?: boolean,
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
  attachCancelSignals = false,
} = {}) {
  refuseUnsupportedIsolatedBrowserCommand({
    checkoutRoot,
    command: 'pnpm sut',
  })
  if (worktreeIsolationApplies(checkoutRoot)) {
    ensureIsolatedE2eDatabase(checkoutRoot, {
      mysqlExecFn,
      schemaExistsFn,
      log,
    })
    await ensureIsolatedE2ePorts(checkoutRoot, { claimRoot: portClaimRoot })
  }
  const { isolated, target } = resolveSutCheckoutTarget({
    checkoutRoot,
    runtimeTarget,
  })
  let owner
  let child
  let effectiveSignal = signal
  let detachCancelSignals = () => undefined
  if (isolated) {
    refuseConflictingSutOverrides(process.env, target)
    assertE2eDatabaseExists(target.database, { existsFn: databaseExistsFn })
    if (retainOwnership) {
      owner = await readHeldSutOwner(checkoutRoot)
    } else {
      await assertNoLiveSutOwner(checkoutRoot)
      await assertAllocatedPortsFree(
        target,
        isPortOccupiedFn ?? defaultIsPortOccupied
      )
      owner = await claimSutOwnership(checkoutRoot)
    }
    log(`Selected database: ${target.database}`)
    log(`Browser origin: ${isolatedBrowserOrigin(target)}`)
    if (!effectiveSignal && attachCancelSignals) {
      const controller = new AbortController()
      effectiveSignal = controller.signal
      const onCancel = () => controller.abort()
      process.once('SIGINT', onCancel)
      process.once('SIGTERM', onCancel)
      detachCancelSignals = () => {
        process.off('SIGINT', onCancel)
        process.off('SIGTERM', onCancel)
      }
    }
  }
  const releaseIfOwned = async () => {
    if (!owner) return
    await releaseFailedIsolatedStart({ child, checkoutRoot })
    owner = undefined
  }
  try {
    if (isolated && retainOwnership) {
      await assertAllocatedPortsFree(
        target,
        isPortOccupiedFn ?? defaultIsPortOccupied
      )
    }
    log(`Starting SUT services... (log: ${logFile})`)
    const spawned = spawnSutServices({
      spawnFn,
      logFile,
      runtimeTarget: target,
      owner,
      checkoutRoot,
    })
    child = spawned.child
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
      checkoutRoot,
      signal: effectiveSignal,
    })
    if (exitCode !== 0) await releaseIfOwned()
    return exitCode
  } catch (error) {
    await releaseIfOwned()
    throw error
  } finally {
    detachCancelSignals()
  }
}

async function defaultIsPortOccupied(port) {
  const result = await checkTcpPort({
    host: '127.0.0.1',
    port,
    timeoutMs: 400,
  })
  return result.ok
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    const code = await runSutStart({ attachCancelSignals: true })
    process.exit(code)
  } catch (e) {
    process.stderr.write(`${e instanceof Error ? e.message : String(e)}\n`)
    process.exit(1)
  }
}
