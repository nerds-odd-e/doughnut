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

async function anyDescendantAlive(rootPid) {
  const descendants = await descendantPidsByParentWalk(rootPid)
  return descendants.some((pid) => processAlive(pid))
}

async function ownedTreeStillRunning(child, pgid) {
  return (
    childStillRunning(child) ||
    processAlive(-pgid) ||
    (await anyDescendantAlive(pgid))
  )
}

async function waitUntilOwnedTreeStops(child, pgid, timeoutMs) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (!(await ownedTreeStillRunning(child, pgid))) return true
    await pause(20)
  }
  return !(await ownedTreeStillRunning(child, pgid))
}

function signalChild(child, signal) {
  try {
    child.kill(signal)
  } catch (error) {
    ignoreMissingProcess(error)
  }
}

async function signalOwnedTree(child, pgid, signal) {
  signalChild(child, signal)
  signalTarget(-pgid, signal)
  const descendants = await descendantPidsByParentWalk(pgid)
  for (const pid of descendants) {
    signalTarget(pid, signal)
  }
}

export async function stopOwnedSutProcessTree(
  child,
  { timeoutMs = 5_000 } = {}
) {
  if (typeof child?.kill !== 'function') return
  const pgid = child.pid
  if (!Number.isInteger(pgid) || pgid <= 0) return
  await signalOwnedTree(child, pgid, 'SIGTERM')
  if (await waitUntilOwnedTreeStops(child, pgid, timeoutMs)) return
  await signalOwnedTree(child, pgid, 'SIGKILL')
  await waitUntilOwnedTreeStops(child, pgid, 1_000)
}
