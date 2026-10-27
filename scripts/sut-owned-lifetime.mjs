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
import { startMcpTlsProxy } from '../e2e_test/support/mcp_tls_proxy.ts'

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
 *   - tlsProxy: the optional HTTPS fixture owned by this lifetime
 *   - ready:    Promise<{ ok, exitCode }> from the readiness wait
 *   - shutdown: async () => stop the owned tree, release ownership and close HTTPS;
 *               the primary target has no owned SUT tree to stop
 *
 * The caller owns shutdown: on a successful ready it may keep the supervisor
 * running (legacy adapter) or shut it down (batch wrapper). On a failed ready,
 * the caller must call shutdown to settle the owned tree when an owner exists.
 * Optional HTTPS is allocated before spawn and closed on failed readiness or shutdown.
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
 *   paidOpenAi?: boolean,
 *   retainOwnership?: boolean,
 *   signal?: AbortSignal,
 *   https?: boolean,
 *   env?: NodeJS.ProcessEnv,
 * }} [opts]
 * @returns {Promise<{ child: import('node:child_process').ChildProcess, target: object, tlsProxy?: import('../e2e_test/support/mcp_tls_proxy.ts').McpTlsProxy, ready: Promise<{ ok: boolean, exitCode: number }>, shutdown: () => Promise<void> }>}
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
  paidOpenAi,
  retainOwnership = false,
  signal,
  https = false,
  env = process.env,
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
  let tlsProxy
  let tlsClose
  const closeTls = () => (tlsClose ??= tlsProxy?.close())
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
      refuseConflictingSutOverrides(env, target)
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
    if (https) tlsProxy = await startMcpTlsProxy(isolatedBrowserOrigin(target))
    log(`Starting SUT services... (log: ${logFile})`)
    const spawned = spawnSutServices({
      spawnFn,
      logFile,
      runtimeTarget: target,
      backendReload,
      paidOpenAi,
      owner: ownerRef.owner,
      checkoutRoot,
      env: tlsProxy
        ? { ...env, DONUT_CANONICAL_ORIGIN: tlsProxy.httpsOrigin }
        : env,
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
    }).then(
      async (result) => {
        if (!result.ok) await closeTls()
        return result
      },
      async (error) => {
        await closeTls()
        throw error
      }
    )

    const shutdown = async () => {
      try {
        await releaseOwnedLifetime({ child, checkoutRoot, ownerRef })
      } finally {
        await closeTls()
      }
    }

    return { child, target, tlsProxy, ready, shutdown }
  } catch (error) {
    if (!admissionReleased) releaseAdmissionIfHeld()
    try {
      await releaseOwnedLifetime({ child, checkoutRoot, ownerRef })
    } finally {
      await closeTls()
    }
    throw error
  }
}
