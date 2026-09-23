/**
 * No-op cancellation handle. The default for `runE2eBatch` so callers that do
 * not need signal handling install no process listeners. The `isMain` entry
 * point wires real SIGINT/SIGTERM handling via `wireBatchCancellation`.
 */
export const NO_CANCEL = {
  signal: undefined,
  isTriggered: () => false,
  onTriggered: () => () => undefined,
  detach: () => undefined,
}

/**
 * Wire SIGINT/SIGTERM to a single AbortController for one batch invocation.
 * The controller's signal aborts the readiness wait; `onTriggered` callbacks
 * stop the Cypress child. One cancellation path — no generic signal framework.
 */
export function wireBatchCancellation(signals = ['SIGINT', 'SIGTERM']) {
  const controller = new AbortController()
  const onSignal = () => controller.abort()
  for (const sig of signals) process.once(sig, onSignal)
  return {
    signal: controller.signal,
    isTriggered: () => controller.signal.aborted,
    onTriggered: (cb) => {
      if (controller.signal.aborted) {
        cb()
        return () => undefined
      }
      controller.signal.addEventListener('abort', cb, { once: true })
      return () => controller.signal.removeEventListener('abort', cb)
    },
    detach: () => {
      for (const sig of signals) process.off(sig, onSignal)
    },
  }
}

/**
 * Observe the supervisor child's exit without polling. Returns a handle
 * whose `exited` promise resolves `true` when the child has exited, and
 * `hasExited()` reports whether the exit was already observed. The promise
 * is created synchronously so an exit in the readiness-to-run window is
 * captured before the Cypress run attaches its own listener.
 */
export function observeChildExit(child) {
  if (!child || typeof child.on !== 'function') {
    // No observable child (test mock): nothing to observe, so `exited` never
    // resolves and `hasExited()` stays false — no spurious service-exit path.
    return {
      exited: new Promise(() => {
        /* never resolves */
      }),
      hasExited: () => false,
    }
  }
  let exited = false
  const promise = new Promise((resolve) => {
    if (child.exitCode != null || child.signalCode) {
      exited = true
      resolve(true)
      return
    }
    child.once('exit', () => {
      exited = true
      resolve(true)
    })
  })
  return { exited: promise, hasExited: () => exited }
}

/**
 * Combine several child-exit observers into one: the combined `exited`
 * resolves when ANY observed child exits, and `hasExited()` reports whether
 * any child has already exited. Used when an invocation owns more than one
 * private mock child — a required mock exiting during the run ends it.
 */
export function combineChildExits(observers) {
  const valid = observers.filter(Boolean)
  if (valid.length === 0) {
    return null
  }
  return {
    exited: Promise.race(valid.map((o) => o.exited)),
    hasExited: () => valid.some((o) => o.hasExited()),
  }
}
