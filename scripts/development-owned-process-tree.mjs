/**
 * Stop a recorded Development process group (no ChildProcess handle).
 * SIGTERM, then SIGKILL if needed — process-tree evidence only.
 */
import { isProcessAlive } from './development-pid.mjs'
import { terminateOwnedProcessTree } from './owned-process-tree-termination.mjs'
import { descendantPidsByParentWalk } from './sut-listener-pids.mjs'

function ignoreMissingProcess(error) {
  if (error.code !== 'ESRCH' && error.code !== 'EPERM') throw error
}

function signalTarget(killFn, target, signal) {
  try {
    killFn(target, signal)
  } catch (error) {
    ignoreMissingProcess(error)
  }
}

function ownedTreeStillRunning(pgid, descendants, isProcessAliveFn) {
  return (
    isProcessAliveFn(pgid) ||
    isProcessAliveFn(-pgid) ||
    descendants.some((pid) => isProcessAliveFn(pid))
  )
}

function signalOwnedDevelopmentTree(killFn, pgid, descendants, signal) {
  for (const pid of descendants) {
    signalTarget(killFn, pid, signal)
  }
  signalTarget(killFn, pgid, signal)
  signalTarget(killFn, -pgid, signal)
}

export async function stopOwnedDevelopmentProcessTree(
  applicationGroupId,
  {
    killFn = process.kill.bind(process),
    isProcessAliveFn = isProcessAlive,
    descendantPidsFn = descendantPidsByParentWalk,
    timeoutMs = 5_000,
  } = {}
) {
  if (!Number.isInteger(applicationGroupId) || applicationGroupId <= 0) return
  await terminateOwnedProcessTree({
    captureDescendants: () => descendantPidsFn(applicationGroupId),
    signalOwnedTree: (descendants, signal) =>
      signalOwnedDevelopmentTree(
        killFn,
        applicationGroupId,
        descendants,
        signal
      ),
    isOwnedTreeRunning: (descendants) =>
      ownedTreeStillRunning(applicationGroupId, descendants, isProcessAliveFn),
    timeoutMs,
    failureMessage:
      'Owned Development process tree did not exit after SIGKILL within the bounded wait',
  })
}
