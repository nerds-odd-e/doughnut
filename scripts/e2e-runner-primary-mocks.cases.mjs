import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'

import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
} from './sut-isolated-fixtures.mjs'
import { SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC } from './isolated-cypress-spec-selection.mjs'
import { spawnIdlePrivateMockHandle } from './isolated-openai-mock-test-fixtures.mjs'
import { sutOwnerLockDir } from './sut-owner.mjs'
import { E2E_RUNNER_OWNS_LIFETIME_ENV_KEY } from './isolated-cypress.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import {
  makeCypressChild,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  healthcheckWaitingForPids,
  trackOwnedTree,
  primaryLifetimeOpts,
} from './e2e-runner-lifetime-fixtures.mjs'

test('primary path: mock-requiring spec does NOT start a wrapper-owned mock (approved null, SUT Mountebank serves the spec)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let mockStarts = 0
  let capturedEnv = null

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...primaryLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      mockStarts += 1
      return spawnIdlePrivateMockHandle()
    },
    spawnCypress: (opts) => {
      capturedEnv = opts.env
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
      })
    },
  })

  assert.equal(code, 0)
  assert.equal(
    mockStarts,
    0,
    'primary path must not start a wrapper-owned mock even for the mock-requiring spec'
  )
  assert.equal(
    capturedEnv && capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
    undefined,
    'Cypress env must not signal wrapper-owned lifetime'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

test('primary path: no private mock started (approved null), Cypress env has no E2E_RUNNER_OWNS_LIFETIME', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let capturedEnv = null
  let mockStarted = false

  const code = await runE2eBatch({
    argv: ['--spec', 'e2e_test/features/foo/**\ne2e_test/features/bar/**'],
    ...primaryLifetimeOpts(checkout.root, standIn),
    startPrivateOpenAiMockFn: async () => {
      mockStarted = true
      return spawnIdlePrivateMockHandle()
    },
    spawnCypress: (opts) => {
      capturedEnv = opts.env
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
      })
    },
  })

  assert.equal(code, 0)
  assert.equal(mockStarted, false, 'no private mock for primary path')
  assert.equal(
    capturedEnv && capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
    undefined,
    'Cypress env must not signal wrapper-owned lifetime'
  )
})
