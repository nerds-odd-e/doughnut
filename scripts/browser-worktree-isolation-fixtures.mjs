import assert from 'node:assert/strict'
import { loadIsolatedE2eStartAllocation } from './browser-worktree-isolation.mjs'
import {
  guardCypressNodeSetup,
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
} from './isolated-cypress.mjs'
import { runSutHealthcheck } from './sut-healthcheck.mjs'
import { runConfiguredStart } from './sut-isolated-fixtures.mjs'

export const isolatedCypressSpec = /only supports|spec selection/i
export const malformedJson = /not valid JSON/i
export const incompleteAllocation =
  /complete E2E allocation|Missing or invalid/i

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

export async function assertReadersRefuseIncompleteAllocation(
  checkoutRoot,
  { refuseStartAllocation = false } = {}
) {
  if (refuseStartAllocation) {
    assert.throws(
      () => loadIsolatedE2eStartAllocation(checkoutRoot),
      incompleteAllocation
    )
  }

  const healthLogs = []
  const healthAccessed = []
  await assert.rejects(
    runHealth(checkoutRoot, healthLogs, healthAccessed),
    incompleteAllocation
  )
  assert.equal(healthLogs.length, 0)
  assert.equal(healthAccessed.length, 0)

  const hooks = { reset: false }
  await assert.rejects(async () => {
    await guardCypressNodeSetup(
      checkoutRoot,
      { specPattern: SUPPORTED_ISOLATED_CYPRESS_SPEC },
      {
        argv: [
          'node',
          'cypress',
          'run',
          '--spec',
          SUPPORTED_ISOLATED_CYPRESS_SPEC,
        ],
      }
    )
    hooks.reset = true
  }, incompleteAllocation)
  assert.equal(hooks.reset, false)
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
