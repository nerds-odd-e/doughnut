import { EventEmitter } from 'node:events'

/** Build a mock child process emitter with controllable exit and stdio. */
export function makeMockChild(pid = 99999) {
  const child = new EventEmitter()
  child.pid = pid
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.killed = false
  child.kill = (signal) => {
    child.killed = signal
  }
  child.unref = () => undefined
  return child
}

/** Spy that records spawn arguments and returns one mock child. */
export function makeStartSpy(pid = 4242) {
  const calls = []
  const child = makeMockChild(pid)
  return {
    calls,
    spawnFn: (...args) => {
      calls.push(args)
      return child
    },
  }
}

/** Spy that captures calls to log / errLog. */
export function makeLogs() {
  const out = []
  const err = []
  return {
    log: (s) => out.push(s),
    errLog: (s) => err.push(s),
    out,
    err,
  }
}

/** A healthcheck function that returns ok=true immediately. */
export async function healthyOnce() {
  return {
    ok: true,
    tcpResults: [],
    readinessResult: { ok: true },
    exitCode: 0,
  }
}

/** A healthcheck function that always returns ok=false. */
export async function neverHealthy() {
  return {
    ok: false,
    tcpResults: [],
    readinessResult: { ok: false },
    exitCode: 1,
  }
}
