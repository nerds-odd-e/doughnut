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
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
  SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC,
} from './isolated-cypress-spec-selection.mjs'
import { spawnIdlePrivateMockHandle } from './isolated-openai-mock-test-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import {
  E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
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

test('private-mock batch: owned mock stays alive across the batch and stops with the invocation', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let mockStarts = 0
  let mockStopCalls = 0
  let mockPid = 0

  const code = await runE2eBatch({
    argv: cypressArgv(
      `${SUPPORTED_ISOLATED_CYPRESS_SPEC},${SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC}`
    ),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      mockStarts += 1
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      // Wrap stop to count calls — the mock must be stopped exactly once
      // (at invocation shutdown), never between specs.
      const realStop = handle.stop
      handle.stop = async () => {
        mockStopCalls += 1
        await realStop()
      }
      return handle
    },
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        // The mock is alive during the Cypress run (no premature release).
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(mockStopCalls, 0, 'mock must not be stopped mid-batch')
      }),
  })

  assert.equal(code, 0)
  assert.equal(mockStarts, 1, 'mock must start exactly once for the batch')
  assert.equal(mockStopCalls, 1, 'mock must be stopped once at invocation end')
  assert.equal(isPidAlive(mockPid), false)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('private Wikidata-mock batch: owned mock stays alive across the batch and stops with the invocation', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let mockStarts = 0
  let mockStopCalls = 0
  let mockPid = 0
  let capturedEnv = null

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateWikidataMockFn: async () => {
      mockStarts += 1
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      const realStop = handle.stop
      handle.stop = async () => {
        mockStopCalls += 1
        await realStop()
      }
      return handle
    },
    spawnCypress: (opts) => {
      capturedEnv = opts.env
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(mockStopCalls, 0, 'mock must not be stopped mid-batch')
      })
    },
  })

  assert.equal(code, 0)
  assert.equal(mockStarts, 1, 'Wikidata mock must start exactly once')
  assert.equal(mockStopCalls, 1, 'Wikidata mock must be stopped once at end')
  assert.equal(isPidAlive(mockPid), false)
  // The Wikidata endpoint is injected into Cypress env under its own key,
  // distinct from the OpenAI endpoint key, and the wrapper owns the lifetime.
  assert.equal(
    capturedEnv && capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
    '1'
  )
  assert.ok(
    capturedEnv && capturedEnv[E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY]
  )
  assert.ok(
    Number.isInteger(
      Number(capturedEnv && capturedEnv[E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY])
    )
  )
  // The OpenAI endpoint key is NOT set for a Wikidata-only batch.
  assert.equal(
    capturedEnv && capturedEnv[E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY],
    undefined
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})
