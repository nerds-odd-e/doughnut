import { EventEmitter } from 'node:events'
import { runSutHealthcheck } from './sut-healthcheck.mjs'
import { runConfiguredStart } from './sut-isolated-fixtures.mjs'

export const isolatedCypressSpec = /only supports|spec selection/i
export const malformedJson = /not valid JSON/i
export const incompleteAllocation =
  /complete E2E allocation|Missing or invalid/i

export function makeRestartSpies() {
  const lsofCalls = []
  const spawnCalls = []
  return {
    lsofCalls,
    spawnCalls,
    execFileFn: (cmd, args, cb) => {
      lsofCalls.push({ cmd, args })
      cb({ code: 1 }, '')
    },
    spawnFn: (...args) => {
      spawnCalls.push(args)
      const child = new EventEmitter()
      queueMicrotask(() => child.emit('close', 0))
      return child
    },
  }
}

function trackingHealthChecks(accessed) {
  return [
    {
      get service() {
        accessed.push('service')
        return 'mountebank'
      },
      host: '127.0.0.1',
      port: 1,
    },
  ]
}

export function runStart(checkoutRoot, spawn) {
  return runConfiguredStart(checkoutRoot, spawn)
}

export async function runHealth(checkoutRoot, logs, accessed) {
  return runSutHealthcheck({
    checkoutRoot,
    tcpChecks: trackingHealthChecks(accessed),
    log: (line) => logs.push(line),
  })
}

export function withCiEnv(t) {
  const previous = process.env.CI
  process.env.CI = 'true'
  t.after(() => {
    if (previous === undefined) {
      delete process.env.CI
    } else {
      process.env.CI = previous
    }
  })
}
