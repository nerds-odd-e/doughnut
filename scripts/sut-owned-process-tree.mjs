/**
 * Stop the spawned SUT process group. The start helper owns that ChildProcess
 * (a detached group leader); do not discover listeners by port.
 */

function signalProcessGroup(pgid, signal) {
  try {
    process.kill(-pgid, signal)
  } catch (error) {
    if (error.code !== 'ESRCH' && error.code !== 'EPERM') throw error
  }
}

function processGroupExists(pgid) {
  try {
    process.kill(-pgid, 0)
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

async function waitUntilOwnedTreeStops(child, pgid, timeoutMs) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (!(childStillRunning(child) || processGroupExists(pgid))) return true
    await pause(20)
  }
  return !(childStillRunning(child) || processGroupExists(pgid))
}

export async function stopOwnedSutProcessTree(
  child,
  { timeoutMs = 5_000 } = {}
) {
  if (typeof child?.kill !== 'function') return
  const pgid = child.pid
  if (!Number.isInteger(pgid) || pgid <= 0) return
  signalProcessGroup(pgid, 'SIGTERM')
  if (await waitUntilOwnedTreeStops(child, pgid, timeoutMs)) return
  signalProcessGroup(pgid, 'SIGKILL')
  await waitUntilOwnedTreeStops(child, pgid, 1_000)
}
