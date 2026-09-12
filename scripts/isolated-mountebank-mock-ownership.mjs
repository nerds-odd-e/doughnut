/**
 * Ownership checks for a runner-owned private Mountebank process group.
 * HTTP readiness alone does not prove ownership. The checks are
 * service-label-parameterized so the same lifecycle adapter can serve any
 * mocked external service; each service-specific starter passes its own
 * `serviceLabel` so failure messages name the responsible service.
 */
import { getListenerPids, processGroupId } from './sut-listener-pids.mjs'

const READY_TIMEOUT_MS = 15_000
const READY_POLL_MS = 50

const DEFAULT_SERVICE_LABEL = 'mock'

function pause(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function ownedProcessGroupId(
  child,
  { serviceLabel = DEFAULT_SERVICE_LABEL } = {}
) {
  const pgid = child?.pid
  if (!Number.isInteger(pgid) || pgid <= 0) {
    throw new Error(
      `Private ${serviceLabel} mock process has no process group id.`
    )
  }
  return pgid
}

export function childStillRunning(child) {
  return child.exitCode === null && child.signalCode === null
}

function refuseForeignListenerMessage(
  port,
  label,
  pid,
  pgid,
  serviceLabel,
  detail = ''
) {
  return (
    `Private ${serviceLabel} mock refuses foreign ${label} listener on port ${port} ` +
    `(pid ${pid} pgid ${pgid}${detail}).`
  )
}

export async function assertOwnedMockListener(
  port,
  ownedPgid,
  label,
  { serviceLabel = DEFAULT_SERVICE_LABEL } = {}
) {
  const pids = await getListenerPids(port)
  if (pids.length === 0) {
    throw new Error(
      `Private ${serviceLabel} mock ${label} port ${port} has no listener owned by this run.`
    )
  }
  for (const pid of pids) {
    const pgid = await processGroupId(pid)
    if (pgid !== ownedPgid) {
      throw new Error(
        refuseForeignListenerMessage(
          port,
          label,
          pid,
          pgid,
          serviceLabel,
          `; owned pgid ${ownedPgid}`
        )
      )
    }
  }
}

/**
 * Serving must be free before the first management mutation creates an imposter.
 * A ready foreign listener is refusal, never adoption.
 */
export async function assertPortFreeBeforeMockMutation(
  port,
  label,
  { serviceLabel = DEFAULT_SERVICE_LABEL } = {}
) {
  const pids = await getListenerPids(port)
  if (pids.length === 0) return
  const pid = pids[0]
  const pgid = await processGroupId(pid)
  throw new Error(
    `${refuseForeignListenerMessage(port, label, pid, pgid, serviceLabel)} ` +
      'Refusing before mock mutation.'
  )
}

export async function waitForOwnedManagementListener(
  child,
  managementPort,
  { serviceLabel = DEFAULT_SERVICE_LABEL } = {}
) {
  const ownedPgid = ownedProcessGroupId(child, { serviceLabel })
  const deadline = Date.now() + READY_TIMEOUT_MS
  while (Date.now() < deadline) {
    if (!childStillRunning(child)) {
      throw new Error(
        `Private ${serviceLabel} mock process exited before management port ${managementPort} was owned ` +
          `(code ${child.exitCode}, signal ${child.signalCode}). Refusing bind/start failure.`
      )
    }
    const pids = await getListenerPids(managementPort)
    if (pids.length > 0) {
      await assertOwnedMockListener(managementPort, ownedPgid, 'management', {
        serviceLabel,
      })
      return
    }
    await pause(READY_POLL_MS)
  }
  throw new Error(
    `Timed out waiting for owned private ${serviceLabel} mock management listener on port ${managementPort}.`
  )
}
