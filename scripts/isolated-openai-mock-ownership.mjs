/**
 * Ownership checks for a runner-owned private OpenAI Mountebank process group.
 * HTTP readiness alone does not prove ownership.
 */
import { getListenerPids, processGroupId } from './sut-listener-pids.mjs'

const READY_TIMEOUT_MS = 15_000
const READY_POLL_MS = 50

function pause(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function ownedProcessGroupId(child) {
  const pgid = child?.pid
  if (!Number.isInteger(pgid) || pgid <= 0) {
    throw new Error('Private OpenAI mock process has no process group id.')
  }
  return pgid
}

export function childStillRunning(child) {
  return child.exitCode === null && child.signalCode === null
}

export async function assertOwnedMockListener(port, ownedPgid, label) {
  const pids = await getListenerPids(port)
  if (pids.length === 0) {
    throw new Error(
      `Private OpenAI mock ${label} port ${port} has no listener owned by this run.`
    )
  }
  for (const pid of pids) {
    const pgid = await processGroupId(pid)
    if (pgid !== ownedPgid) {
      throw new Error(
        `Private OpenAI mock refuses foreign ${label} listener on port ${port} ` +
          `(pid ${pid} pgid ${pgid}; owned pgid ${ownedPgid}).`
      )
    }
  }
}

export async function waitForOwnedManagementListener(child, managementPort) {
  const ownedPgid = ownedProcessGroupId(child)
  const deadline = Date.now() + READY_TIMEOUT_MS
  while (Date.now() < deadline) {
    if (!childStillRunning(child)) {
      throw new Error(
        `Private OpenAI mock process exited before management port ${managementPort} was owned ` +
          `(code ${child.exitCode}, signal ${child.signalCode}). Refusing bind/start failure.`
      )
    }
    const pids = await getListenerPids(managementPort)
    if (pids.length > 0) {
      await assertOwnedMockListener(managementPort, ownedPgid, 'management')
      return
    }
    await pause(READY_POLL_MS)
  }
  throw new Error(
    `Timed out waiting for owned private OpenAI mock management listener on port ${managementPort}.`
  )
}
