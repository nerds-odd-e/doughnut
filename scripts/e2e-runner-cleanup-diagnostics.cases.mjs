import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  startLiveOwner,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
  SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC,
} from './isolated-cypress-spec-selection.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import {
  makeCypressChild,
  makeCypressLaunchError,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import { readyLifetimeStandIn } from './e2e-runner-lifetime-fixtures.mjs'

for (const [failure, cypressCode] of [
  ['mock', 0],
  ['lease', 0],
  ['all', 7],
]) {
  test(`${failure} cleanup failures remain visible and preserve Cypress exit ${cypressCode}`, async (t) => {
    const checkout = makePrimaryCheckout(t)
    writeIsolatedConfig(checkout.root)
    const { server } = await startLiveOwner(checkout.root, t)
    const errors = []
    const stopped = []
    const code = await runE2eBatch({
      argv: cypressArgv(
        `${SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC},${SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC}`
      ),
      checkoutRoot: checkout.root,
      errLog: (message) => errors.push(message),
      startLifetime: async () => ({
        ...readyLifetimeStandIn(),
        shutdown: async () => {
          stopped.push('SUT')
          if (cypressCode) throw new Error('owned tree did not exit')
        },
      }),
      startPrivateOpenAiMockFn: async () => ({
        endpoint: {},
        stop: async () => {
          stopped.push('OpenAI')
          if (failure !== 'lease') throw new Error('OpenAI did not exit')
        },
      }),
      startPrivateWikidataMockFn: async () => ({
        endpoint: {},
        stop: async () => {
          stopped.push('Wikidata')
          if (failure !== 'mock') {
            server.removeAllListeners('request')
            server.on('request', (_req, res) => {
              res.writeHead(500, { 'Content-Type': 'application/json' })
              res.end(
                JSON.stringify({ ok: false, error: 'lease release refused' })
              )
            })
          }
          if (failure !== 'lease') throw new Error('Wikidata did not exit')
        },
      }),
      spawnCypress: () => makeCypressChild(cypressCode),
    })
    assert.equal(code, cypressCode || 1)
    assert.deepEqual(stopped, ['OpenAI', 'Wikidata', 'SUT'])
    if (failure !== 'lease') {
      assert.match(errors.join('\n'), /OpenAI did not exit/)
      assert.match(errors.join('\n'), /Wikidata did not exit/)
    }
    if (failure !== 'mock')
      assert.match(errors.join('\n'), /lease release refused/)
    if (cypressCode) assert.match(errors.join('\n'), /owned tree did not exit/)
  })
}

test('Cypress launch diagnostics survive a cleanup failure', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const errors = []
  const code = await runE2eBatch({
    argv: cypressArgv(),
    checkoutRoot: checkout.root,
    errLog: (message) => errors.push(message),
    startLifetime: async () => ({
      ...readyLifetimeStandIn(),
      shutdown: async () => {
        throw new Error('owned tree did not exit')
      },
    }),
    spawnCypress: makeCypressLaunchError(new Error('Cypress launch failed')),
  })
  assert.equal(code, 1)
  assert.match(errors.join('\n'), /Cypress launch failed/)
  assert.match(errors.join('\n'), /owned tree did not exit/)
})
