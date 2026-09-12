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
  readPresentWorktreeLocalConfig,
  refuseUnsupportedIsolatedBrowserCommand,
  worktreeIsolationApplies,
} from './browser-worktree-isolation.mjs'
import { ensureIsolatedE2eDatabase } from './sut-e2e-database.mjs'
import { ensureIsolatedE2ePorts } from './sut-e2e-ports.mjs'
import { isTcpPortOccupied } from './sut-healthcheck.mjs'
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
import { initializeWorktreeIdentity } from './worktree-identity.mjs'
import { holdRetirementAdmission } from './worktree-retirement-admission.mjs'

export { LOG_FILE, PID_FILE, spawnSutServices, writePidFile }

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

async function releaseOwnedLifetime({ child, checkoutRoot, ownerRef }) {
  if (!ownerRef.owner) return
  await stopOwnedSutProcessTree(child)
  await releaseSutOwnership(checkoutRoot)
  ownerRef.owner = undefined
}

/**
 * Allocate the isolated/primary SUT target, spawn the detached supervisor, and
 * expose its running lifetime to an in-process caller.
 *
 * Returns a handle with:
 *   - child:    the running supervisor ChildProcess (detached group leader)
 *   - target:   the resolved runtime target
 *   - ready:    Promise<{ ok, exitCode }> from the readiness wait
 *   - shutdown: async () => stop the owned tree and release ownership; no-op for
 *               the primary (non-isolated) target where no ownership was claimed
 *
 * The caller owns shutdown: on a successful ready it may keep the supervisor
 * running (legacy adapter) or shut it down (batch wrapper). On a failed ready,
 * the caller must call shutdown to settle the owned tree when an owner exists.
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
 * @returns {Promise<{ child: import('node:child_process').ChildProcess, target: object, ready: Promise<{ ok: boolean, exitCode: number }>, shutdown: () => Promise<void> }>}
 */
export async function startOwnedSutLifetime({
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
  // Retained-owner restart already holds SUT ownership; do not take the gate again.
  const releaseAdmissionIfHeld =
    worktreeIsolationApplies(checkoutRoot) && !retainOwnership
      ? holdRetirementAdmission(checkoutRoot)
      : () => undefined
  const ownerRef = { owner: undefined }
  let child
  let target
  let admissionReleased = false
  try {
    if (
      worktreeIsolationApplies(checkoutRoot) &&
      readPresentWorktreeLocalConfig(checkoutRoot) === null
    ) {
      initializeWorktreeIdentity(checkoutRoot)
    }
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
    const { isolated, target: resolvedTarget } = resolveSutCheckoutTarget({
      checkoutRoot,
      runtimeTarget,
    })
    target = resolvedTarget
    if (isolated) {
      refuseConflictingSutOverrides(process.env, target)
      assertE2eDatabaseExists(target.database, { existsFn: databaseExistsFn })
      if (retainOwnership) {
        ownerRef.owner = await readHeldSutOwner(checkoutRoot)
      } else {
        await assertNoLiveSutOwner(checkoutRoot)
        await assertAllocatedPortsFree(
          target,
          isPortOccupiedFn ?? isTcpPortOccupied
        )
        ownerRef.owner = await claimSutOwnership(checkoutRoot)
      }
      releaseAdmissionIfHeld()
      admissionReleased = true
      log(`Selected database: ${target.database}`)
      log(`Browser origin: ${isolatedBrowserOrigin(target)}`)
    } else {
      releaseAdmissionIfHeld()
      admissionReleased = true
    }
    if (isolated && retainOwnership) {
      await assertAllocatedPortsFree(
        target,
        isPortOccupiedFn ?? isTcpPortOccupied
      )
    }
    log(`Starting SUT services... (log: ${logFile})`)
    const spawned = spawnSutServices({
      spawnFn,
      logFile,
      runtimeTarget: target,
      owner: ownerRef.owner,
      checkoutRoot,
    })
    child = spawned.child
    await writePidFile(child.pid, { pidFile })

    const ready = waitForSutHealthy({
      child,
      timeoutMs,
      pollMs,
      logFile,
      log,
      errLog,
      healthcheckFn,
      runtimeTarget: target,
      checkoutRoot,
      signal,
    })

    const shutdown = async () => {
      await releaseOwnedLifetime({ child, checkoutRoot, ownerRef })
    }

    return { child, target, ready, shutdown }
  } catch (error) {
    if (!admissionReleased) releaseAdmissionIfHeld()
    await releaseOwnedLifetime({ child, checkoutRoot, ownerRef })
    throw error
  }
}

/**
 * Legacy start adapter: spawn services, write PID file, wait for health, and
 * return an exit code. On a healthy start the detached supervisor keeps
 * running (so `pnpm sut:restart` and signal-based shutdown still work). On a
 * failed isolated start the owned tree and ownership are released.
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
  let effectiveSignal = signal
  let detachCancelSignals = () => undefined
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
  try {
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
      signal: effectiveSignal,
    })
    const { exitCode } = await lifetime.ready
    if (exitCode !== 0) await lifetime.shutdown()
    return exitCode
  } finally {
    detachCancelSignals()
  }
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
