#!/usr/bin/env node
/**
 * Restart the Development stack only when listeners are proven owned by the
 * recorded `dev.pid` process group. Unowned or unverifiable listeners are left
 * running and the command fails loudly (ADR 0006 / ADR 0007).
 */
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { worktreeIsolationApplies } from './browser-worktree-isolation.mjs'
import { runDevStart } from './dev-start.mjs'
import {
  isProcessAlive,
  readRecordedDevelopmentPid,
} from './development-pid.mjs'
import { stopOwnedDevelopmentProcessTree } from './development-owned-process-tree.mjs'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import {
  applicationPorts,
  listOccupiedApplicationPorts,
} from './local-runtime-target.mjs'
import { isTcpPortOccupied } from './sut-healthcheck.mjs'
import {
  descendantPidsByParentWalk,
  getListenerPids,
  isOwnedByApplicationTree,
} from './sut-listener-pids.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

/**
 * Prove every listener on Development application ports belongs to the recorded
 * live `dev.pid` process group. Process-tree evidence only — no ownership sockets.
 *
 * @returns {Promise<
 *   | { ok: true, applicationGroupId: number }
 *   | { ok: false, reason: string }
 * >}
 */
export async function verifyOwnedDevelopmentGroup({
  runtimeTarget = DEVELOPMENT_RUNTIME_TARGET,
  isProcessAliveFn = isProcessAlive,
  getListenerPidsFn = getListenerPids,
  isOwnedByApplicationTreeFn = isOwnedByApplicationTree,
  listenersByPort,
} = {}) {
  const recorded = readRecordedDevelopmentPid(runtimeTarget.pidFile)
  if (recorded.kind === 'absent') {
    return {
      ok: false,
      reason:
        'Development ports are occupied but `dev.pid` is missing. ' +
        'Refusing restart; listeners were not signalled.',
    }
  }
  if (recorded.kind === 'invalid') {
    return {
      ok: false,
      reason:
        'Development ports are occupied but `dev.pid` is missing or unreadable. ' +
        'Refusing restart; listeners were not signalled.',
    }
  }

  const { pid: applicationGroupId } = recorded
  if (!isProcessAliveFn(applicationGroupId)) {
    return {
      ok: false,
      reason:
        `Development ports are occupied but recorded process ${applicationGroupId} in ${runtimeTarget.pidFile} is stale. ` +
        'Refusing restart; listeners were not signalled.',
    }
  }

  /** @type {Map<number, number[]>} */
  const byPort =
    listenersByPort ??
    (await listenersByDevelopmentPort(runtimeTarget, getListenerPidsFn))

  /** @type {number[]} */
  const foreignOrUnverified = []
  for (const pids of byPort.values()) {
    for (const listenerPid of pids) {
      if (
        !(await isOwnedByApplicationTreeFn(listenerPid, applicationGroupId))
      ) {
        foreignOrUnverified.push(listenerPid)
      }
    }
  }

  if (!hasAnyListener(byPort)) {
    return {
      ok: false,
      reason:
        'Development ownership could not be verified from process evidence. ' +
        'Refusing restart; listeners were not signalled.',
    }
  }

  if (foreignOrUnverified.length > 0) {
    return {
      ok: false,
      reason:
        `Development listeners [${foreignOrUnverified.join(', ')}] are not owned by recorded process group ${applicationGroupId}. ` +
        'Refusing restart; listeners were not signalled.',
    }
  }

  return { ok: true, applicationGroupId }
}

/**
 * Collect listener PIDs across Development application ports.
 *
 * @returns {Promise<Map<number, number[]>>} port → pids
 */
async function listenersByDevelopmentPort(runtimeTarget, getListenerPidsFn) {
  /** @type {Map<number, number[]>} */
  const byPort = new Map()
  for (const port of applicationPorts(runtimeTarget)) {
    byPort.set(port, await getListenerPidsFn(port))
  }
  return byPort
}

function hasAnyListener(byPort) {
  for (const pids of byPort.values()) {
    if (pids.length > 0) return true
  }
  return false
}

function pause(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitUntilDevelopmentPortsFree(
  runtimeTarget,
  { isPortOccupiedFn = isTcpPortOccupied, timeoutMs = 15_000 } = {}
) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const occupied = await listOccupiedApplicationPorts(
      runtimeTarget,
      isPortOccupiedFn
    )
    if (occupied.length === 0) return
    await pause(40)
  }
  throw new Error(
    'Timed out waiting for Development ports to become free after stopping the owned process group.'
  )
}

/**
 * Restart Development only when ownership is proven. An idle free target
 * starts via `pnpm dev` / `runDevStart` without signalling.
 *
 * @returns {Promise<number>}
 */
export async function runDevRestart({
  checkoutRoot = repoRoot,
  runtimeTarget = DEVELOPMENT_RUNTIME_TARGET,
  isProcessAliveFn = isProcessAlive,
  getListenerPidsFn = getListenerPids,
  isOwnedByApplicationTreeFn = isOwnedByApplicationTree,
  killFn = process.kill.bind(process),
  runDevStartFn = runDevStart,
  isPortOccupiedFn = isTcpPortOccupied,
  descendantPidsFn = descendantPidsByParentWalk,
  stopTimeoutMs = 5_000,
  portsFreeTimeoutMs = 15_000,
} = {}) {
  if (worktreeIsolationApplies(checkoutRoot)) {
    throw new Error(
      'Development (`pnpm dev:restart`) is only supported in the unconfigured primary checkout. ' +
        'This checkout uses worktree isolation; refusing restart. Resources were left unchanged.'
    )
  }

  const byPort = await listenersByDevelopmentPort(
    runtimeTarget,
    getListenerPidsFn
  )
  if (!hasAnyListener(byPort)) {
    return runDevStartFn({ checkoutRoot, runtimeTarget })
  }

  const owned = await verifyOwnedDevelopmentGroup({
    runtimeTarget,
    isProcessAliveFn,
    getListenerPidsFn,
    isOwnedByApplicationTreeFn,
    listenersByPort: byPort,
  })
  if (!owned.ok) {
    throw new Error(owned.reason)
  }

  await stopOwnedDevelopmentProcessTree(owned.applicationGroupId, {
    killFn,
    isProcessAliveFn,
    descendantPidsFn,
    timeoutMs: stopTimeoutMs,
  })
  await waitUntilDevelopmentPortsFree(runtimeTarget, {
    isPortOccupiedFn,
    timeoutMs: portsFreeTimeoutMs,
  })
  return runDevStartFn({ checkoutRoot, runtimeTarget })
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    const code = await runDevRestart()
    process.exit(code)
  } catch (error) {
    process.stderr.write(
      `${error instanceof Error ? error.message : String(error)}\n`
    )
    process.exit(1)
  }
}
