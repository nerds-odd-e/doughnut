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
import { runE2eBatch } from './e2e-runner.mjs'
import {
  makeCypressChild,
  makeLiveCypressChild,
  manualCancel,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  isolatedLifetimeOpts,
  healthcheckWaitingForPids,
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

test('private-mock startup failure: cleans partial owned work (mock + SUT) and returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressSpawned = false

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      throw new Error('private mock startup failed')
    },
    spawnCypress: () => {
      cypressSpawned = true
      return makeCypressChild(0)
    },
  })

  assert.equal(code, 1)
  assert.equal(
    cypressSpawned,
    false,
    'Cypress must not run after mock startup failure'
  )
  state.owned = await waitForOwnedPids(standIn.pidsFile)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('unexpected mock exit during Cypress run: terminates Cypress + settles owned services with failure', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressChild = null
  let mockPid = 0

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      return handle
    },
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(isPidAlive(state.owned.leader), true)
        // Simulate the private mock exiting unexpectedly during the run.
        process.kill(mockPid, 'SIGKILL')
      })
      return cypressChild
    },
  })

  assert.equal(code, 1, 'mock exit must end the run with visible failure')
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop after the mock exit'
  )
  assert.equal(isPidAlive(mockPid), false)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('cancel during a mock-requiring batch: mock + SUT are stopped as part of invocation cleanup', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let mockPid = 0

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      return handle
    },
    spawnCypress: () =>
      makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(mockPid), true)
        cancel.trigger()
      }),
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.equal(isPidAlive(mockPid), false, 'mock must be stopped on cancel')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})
