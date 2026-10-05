/**
 * This checkout's running Development stack, as the process table shows it:
 * every Development services process started from the checkout, with its tree.
 * `pnpm dev`, `pnpm dev:stop`, and `pnpm dev:restart` share this ownership rule.
 */
import { developmentServicesScript } from './development-runtime.mjs'
import { terminateOwnedProcessTree } from './owned-process-tree-termination.mjs'
import { descendantPidsByParentWalk } from './sut-listener-pids.mjs'
import { listProcessTable } from './worktree-retirement-checkout-processes.mjs'

export async function findDevelopmentServicesPids(checkoutRoot) {
  const script = developmentServicesScript(checkoutRoot)
  const rows = await listProcessTable()
  return rows
    .filter((row) => row.command.includes(script))
    .map((row) => row.pid)
}

function isProcessAlive(pid) {
  try {
    process.kill(pid, 0)
    return true
  } catch (error) {
    if (error.code === 'ESRCH') return false
    throw error
  }
}

function signalTarget(target, signal) {
  try {
    process.kill(target, signal)
  } catch (error) {
    if (error.code !== 'ESRCH' && error.code !== 'EPERM') throw error
  }
}

function servicesTreeStillRunning(servicesPid, descendants) {
  return (
    isProcessAlive(servicesPid) ||
    isProcessAlive(-servicesPid) ||
    descendants.some((pid) => isProcessAlive(pid))
  )
}

function signalServicesTree(servicesPid, descendants, signal) {
  for (const pid of descendants) {
    signalTarget(pid, signal)
  }
  signalTarget(servicesPid, signal)
  signalTarget(-servicesPid, signal)
}

/**
 * SIGTERM, then SIGKILL if needed, the services process, its process group,
 * and its parent-walk descendants.
 */
async function stopServicesTree(servicesPid) {
  await terminateOwnedProcessTree({
    captureDescendants: () => descendantPidsByParentWalk(servicesPid),
    signalOwnedTree: (descendants, signal) =>
      signalServicesTree(servicesPid, descendants, signal),
    isOwnedTreeRunning: (descendants) =>
      servicesTreeStillRunning(servicesPid, descendants),
    failureMessage:
      'Development services process tree did not exit after SIGKILL within the bounded wait',
  })
}

/**
 * Stop every Development services process tree started from `checkoutRoot`.
 *
 * @returns {Promise<number[]>} the services PIDs stopped
 */
export async function stopDevelopmentServices(checkoutRoot) {
  const pids = await findDevelopmentServicesPids(checkoutRoot)
  for (const pid of pids) {
    await stopServicesTree(pid)
  }
  return pids
}
