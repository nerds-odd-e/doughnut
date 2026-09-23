/**
 * Allocate the isolated/primary SUT target, spawn its detached supervisor, and
 * expose readiness and shutdown to the invocation that owns the lifetime.
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
 *   backendReload?: boolean,
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
  backendReload,
  retainOwnership = false,
  signal,
} = {}) {
  // A retained-owner start already holds SUT ownership; do not take the gate again.
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
      backendReload,
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
