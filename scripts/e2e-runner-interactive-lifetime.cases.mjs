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
import { SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC } from './isolated-cypress-spec-selection.mjs'
import { spawnIdlePrivateMockHandle } from './isolated-openai-mock-test-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import {
  E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_MOCK_PGID_ENV_KEY,
  E2E_RUNNER_OWNS_LIFETIME_ENV_KEY,
} from './isolated-cypress.mjs'
import { runE2eInteractive } from './e2e-runner.mjs'
import {
  cypressArgv,
  makeOpenCypressChild,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  isolatedLifetimeOpts,
  healthcheckWaitingForPids,
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

test('interactive session: one owned stack shared across selections/reruns, no after-spec teardown, cleanup on close', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eInteractive({
    argv: [], // pure browsing: no preselected spec, no mock
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeOpenCypressChild(async (session) => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        // "Selection" (first spec run within the open session): stack alive.
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
        // "Rerun" (second spec run): the SAME stack is still alive — no
        // after-spec teardown happened between selections.
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
        // Closing Cypress ends the session.
        session.close()
      }),
  })

  assert.equal(code, 0, 'a clean close must return 0')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('interactive session: owned mock stays alive across reruns (no after-spec teardown), stopped on close', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let mockStarts = 0
  let mockStopCalls = 0
  let mockPid = 0

  const code = await runE2eInteractive({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
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
    spawnCypress: () =>
      makeOpenCypressChild(async (session) => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        // First selection of the mock-requiring spec: mock + stack alive.
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(mockStopCalls, 0, 'mock must not be stopped after a spec')
        // Rerun: the SAME mock + stack are still alive — no after-spec teardown.
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(
          mockStopCalls,
          0,
          'mock must not be stopped between reruns'
        )
        session.close()
      }),
  })

  assert.equal(code, 0)
  assert.equal(mockStarts, 1, 'mock must start exactly once for the session')
  assert.equal(mockStopCalls, 1, 'mock must be stopped once on close')
  assert.equal(isPidAlive(mockPid), false)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('interactive session: endpoint configuration verified before browser launch (owned endpoint injected into Cypress env)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let capturedEnv = null
  let mockEndpoint = null

  const code = await runE2eInteractive({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      const handle = spawnIdlePrivateMockHandle()
      mockEndpoint = handle.endpoint
      return handle
    },
    spawnCypress: (opts) => {
      capturedEnv = opts.env
      return makeOpenCypressChild(async (session) => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        session.close()
      })
    },
  })

  assert.equal(code, 0)
  assert.ok(capturedEnv, 'Cypress env must be supplied before browser launch')
  assert.equal(
    capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
    '1',
    'plugin must be in adapter mode (wrapper owns the lifetime)'
  )
  const injected = JSON.parse(capturedEnv[E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY])
  assert.deepEqual(
    injected,
    mockEndpoint,
    'owned endpoint injected before launch'
  )
  assert.ok(
    Number.isInteger(Number(capturedEnv[E2E_RUNNER_MOCK_PGID_ENV_KEY])),
    'owned mock pgid injected before launch'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})
