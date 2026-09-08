/**
 * Stop the spawned SUT process group. The start helper owns that ChildProcess
 * (a detached group leader); do not discover listeners by port. Also terminate
 * PPID descendants of the supervisor so Gradle-forked Boot JVMs (own PGID) exit.
 */
import { descendantPidsByParentWalk } from './sut-listener-pids.mjs'

function ignoreMissingProcess(error) {
  if (error.code !== 'ESRCH' && error.code !== 'EPERM') throw error
}

function signalTarget(target, signal) {
  try {
    process.kill(target, signal)
  } catch (error) {
    ignoreMissingProcess(error)
  }
}

function processAlive(target) {
  try {
    process.kill(target, 0)
    return true
  } catch (error) {
    if (error.code === 'ESRCH') return false
    if (error.code === 'EPERM') return true
    throw error
  }
}

function childStillRunning(child) {
  return child.exitCode === null && child.signalCode === null
}

function pause(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function anyCapturedAlive(descendants) {
  return descendants.some((pid) => processAlive(pid))
}

function ownedTreeStillRunning(child, pgid, descendants) {
  return (
    childStillRunning(child) ||
    processAlive(-pgid) ||
    anyCapturedAlive(descendants)
  )
}

async function waitUntilOwnedTreeStops(child, pgid, descendants, timeoutMs) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (!ownedTreeStillRunning(child, pgid, descendants)) return true
    await pause(20)
  }
  return !ownedTreeStillRunning(child, pgid, descendants)
}

function signalChild(child, signal) {
  try {
    child.kill(signal)
  } catch (error) {
    ignoreMissingProcess(error)
  }
}

function signalOwnedTree(child, pgid, descendants, signal) {
  for (const pid of descendants) {
    signalTarget(pid, signal)
  }
  signalChild(child, signal)
  signalTarget(-pgid, signal)
}

export async function stopOwnedSutProcessTree(
  child,
  { timeoutMs = 5_000 } = {}
) {
  if (typeof child?.kill !== 'function') return
  const pgid = child.pid
  if (!Number.isInteger(pgid) || pgid <= 0) return
  // Capture before signals can reparent separately grouped children away from
  // the PPID walk; reuse that set for signaling and completion checks.
  const descendants = await descendantPidsByParentWalk(pgid)
  signalOwnedTree(child, pgid, descendants, 'SIGTERM')
  if (await waitUntilOwnedTreeStops(child, pgid, descendants, timeoutMs)) return
  signalOwnedTree(child, pgid, descendants, 'SIGKILL')
  if (await waitUntilOwnedTreeStops(child, pgid, descendants, 1_000)) return
  throw new Error(
    'Owned SUT process tree did not exit after SIGKILL within the bounded wait'
  )
}
