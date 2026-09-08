import { EventEmitter } from 'node:events'

/** Build a mock child process emitter with controllable exit. */
export function makeMockChild(pid = 99999) {
  const child = new EventEmitter()
  child.pid = pid
  child.unref = () => undefined
  return child
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
