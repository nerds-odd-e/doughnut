import { EventEmitter } from 'node:events'
import { SUPPORTED_ISOLATED_CYPRESS_SPEC } from './isolated-cypress-spec-selection.mjs'

export function makeCypressChild(exitCode, onSpawn) {
  const child = new EventEmitter()
  child.pid = 0
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.kill = () => undefined
  child.unref = () => undefined
  // emit exit on next tick so listeners attach first; onSpawn runs first so
  // tests can observe the live owned tree before shutdown.
  queueMicrotask(async () => {
    try {
      await onSpawn?.()
    } catch {
      // observation best-effort; do not block the exit
    }
    child.emit('exit', exitCode, null)
  })
  return child
}

export function makeLiveCypressChild(onSpawn) {
  const child = new EventEmitter()
  child.pid = 0
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.killed = false
  child.kill = (signal) => {
    if (child.killed) return
    child.killed = signal || 'SIGTERM'
    queueMicrotask(() => child.emit('exit', null, child.killed))
  }
  child.unref = () => undefined
  if (onSpawn) {
    queueMicrotask(async () => {
      try {
        await onSpawn()
      } catch {
        // observation best-effort
      }
    })
  }
  return child
}

export function manualCancel() {
  const controller = new AbortController()
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
    detach: () => undefined,
    trigger: () => controller.abort(),
  }
}

export function makeCypressLaunchError(error, onSpawn) {
  return () => {
    const child = new EventEmitter()
    child.pid = 0
    child.kill = () => undefined
    child.unref = () => undefined
    queueMicrotask(async () => {
      try {
        await onSpawn?.()
      } catch {
        // observation best-effort
      }
      child.emit('error', error)
    })
    return child
  }
}

export function cypressArgv(spec = SUPPORTED_ISOLATED_CYPRESS_SPEC) {
  return ['--spec', spec]
}

export function makeStickyCypressChild(onSpawn) {
  const child = new EventEmitter()
  child.pid = 0
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.killed = false
  child.kill = (signal) => {
    if (child.killed) return
    if (signal === 'SIGKILL') {
      child.killed = signal
      queueMicrotask(() => child.emit('exit', null, child.killed))
    }
    // SIGTERM (and any other signal) is ignored: Electron does not exit on it.
  }
  child.unref = () => undefined
  if (onSpawn) {
    queueMicrotask(async () => {
      try {
        await onSpawn()
      } catch {
        // observation best-effort
      }
    })
  }
  return child
}

export function makeOpenCypressChild(onSpawn) {
  const child = new EventEmitter()
  child.pid = 0
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.killed = false
  child.kill = (signal) => {
    if (child.killed) return
    child.killed = signal || 'SIGTERM'
    queueMicrotask(() => child.emit('exit', null, child.killed))
  }
  child.close = () => {
    if (child.killed) return
    child.killed = true
    queueMicrotask(() => child.emit('exit', 0, null))
  }
  child.unref = () => undefined
  queueMicrotask(async () => {
    try {
      await onSpawn?.(child)
    } catch {
      // observation best-effort; do not block the exit
    }
  })
  return child
}
