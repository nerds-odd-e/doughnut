function pause(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitUntilOwnedTreeStops(isOwnedTreeRunning, timeoutMs) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (!isOwnedTreeRunning()) return true
    await pause(20)
  }
  return !isOwnedTreeRunning()
}

export async function terminateOwnedProcessTree({
  captureDescendants,
  signalOwnedTree,
  isOwnedTreeRunning,
  timeoutMs = 5_000,
  failureMessage,
}) {
  // Capture before signals can reparent descendants away from ownership walks.
  const descendants = await captureDescendants()
  const treeIsRunning = () => isOwnedTreeRunning(descendants)
  signalOwnedTree(descendants, 'SIGTERM')
  if (await waitUntilOwnedTreeStops(treeIsRunning, timeoutMs)) return
  signalOwnedTree(descendants, 'SIGKILL')
  if (await waitUntilOwnedTreeStops(treeIsRunning, 1_000)) return
  throw new Error(failureMessage)
}
