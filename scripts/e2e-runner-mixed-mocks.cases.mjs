import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
  SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC,
} from './isolated-cypress-spec-selection.mjs'
import { spawnIdlePrivateMockHandle } from './isolated-openai-mock-test-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import {
  E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_MOCK_PGID_ENV_KEY,
  E2E_RUNNER_OWNS_LIFETIME_ENV_KEY,
  E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY,
} from './isolated-cypress.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import {
  makeCypressChild,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  isolatedLifetimeOpts,
  healthcheckWaitingForPids,
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

test('mixed-resource batch: OpenAI + Wikidata mocks both start under one invocation, survive until exit, and stop together', async (t) => {
  // Slice 7 gap: a batch whose files union both `requiresPrivateOpenAiMock`
  // and `requiresPrivateWikidataMock` must start ONE owned management process
  // per service under a single lease, inject each endpoint under its own env
  // key, keep both mocks alive across the whole batch (no after-spec release),
  // and stop both at invocation exit. The registry unions batch requirements;
  // this proves the union survives the multi-file transition without one
  // service's endpoint leaking into the other's env key.
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let openAiStarts = 0
  let wikidataStarts = 0
  let openAiStopCalls = 0
  let wikidataStopCalls = 0
  let openAiPid = 0
  let wikidataPid = 0
  let openAiEndpoint = null
  let wikidataEndpoint = null
  let capturedEnv = null

  const code = await runE2eBatch({
    argv: cypressArgv(
      `${SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC},${SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC}`
    ),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      openAiStarts += 1
      const handle = spawnIdlePrivateMockHandle({
        managementUrl: 'http://127.0.0.1:18025',
        servingPort: 18001,
      })
      openAiPid = handle.child.pid
      openAiEndpoint = handle.endpoint
      const realStop = handle.stop
      handle.stop = async () => {
        openAiStopCalls += 1
        await realStop()
      }
      return handle
    },
    startPrivateWikidataMockFn: async () => {
      wikidataStarts += 1
      const handle = spawnIdlePrivateMockHandle({
        managementUrl: 'http://127.0.0.1:18026',
        servingPort: 18002,
      })
      wikidataPid = handle.child.pid
      wikidataEndpoint = handle.endpoint
      const realStop = handle.stop
      handle.stop = async () => {
        wikidataStopCalls += 1
        await realStop()
      }
      return handle
    },
    spawnCypress: (opts) => {
      capturedEnv = opts.env
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        // Both mocks are alive during the Cypress run (no premature release).
        assert.equal(isPidAlive(openAiPid), true)
        assert.equal(isPidAlive(wikidataPid), true)
        assert.equal(
          openAiStopCalls,
          0,
          'OpenAI mock must not be stopped mid-batch'
        )
        assert.equal(
          wikidataStopCalls,
          0,
          'Wikidata mock must not be stopped mid-batch'
        )
      })
    },
  })

  assert.equal(code, 0)
  // Each service's mock starts exactly once for the whole batch.
  assert.equal(openAiStarts, 1, 'OpenAI mock must start exactly once')
  assert.equal(wikidataStarts, 1, 'Wikidata mock must start exactly once')
  // Both mocks are stopped once at invocation end.
  assert.equal(openAiStopCalls, 1, 'OpenAI mock must be stopped once at end')
  assert.equal(
    wikidataStopCalls,
    1,
    'Wikidata mock must be stopped once at end'
  )
  assert.equal(isPidAlive(openAiPid), false)
  assert.equal(isPidAlive(wikidataPid), false)
  // The wrapper owns the lifetime and injects each endpoint under its own
  // env key — the union of resources, with no cross-service leakage.
  assert.equal(
    capturedEnv && capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
    '1'
  )
  assert.ok(
    capturedEnv && capturedEnv[E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY],
    'OpenAI endpoint injected under its own env key'
  )
  assert.ok(
    capturedEnv && capturedEnv[E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY],
    'Wikidata endpoint injected under its own env key'
  )
  // The two endpoints are distinct (no aliasing between services).
  const injectedOpenAi = JSON.parse(
    capturedEnv[E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY]
  )
  const injectedWikidata = JSON.parse(
    capturedEnv[E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY]
  )
  assert.deepEqual(injectedOpenAi, openAiEndpoint)
  assert.deepEqual(injectedWikidata, wikidataEndpoint)
  assert.notDeepEqual(injectedOpenAi, injectedWikidata)
  assert.ok(Number.isInteger(Number(capturedEnv[E2E_RUNNER_MOCK_PGID_ENV_KEY])))
  assert.ok(
    Number.isInteger(Number(capturedEnv[E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY]))
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})
