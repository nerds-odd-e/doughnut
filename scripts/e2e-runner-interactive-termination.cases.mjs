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
import { runE2eInteractive } from './e2e-runner.mjs'
import {
  manualCancel,
  cypressArgv,
  makeStickyCypressChild,
  makeOpenCypressChild,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  isolatedLifetimeOpts,
  healthcheckWaitingForPids,
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

test('interactive session: cancellation stops Cypress + stack + mock, returns nonzero, zero survivors', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let mockPid = 0
  let openChild = null

  const code = await runE2eInteractive({
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
      openChild = makeOpenCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(mockPid), true)
        cancel.trigger()
      })
      return openChild
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.ok(
    openChild && openChild.killed,
    'Cypress child must be signalled to stop'
  )
  assert.equal(isPidAlive(mockPid), false, 'mock must be stopped on cancel')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('interactive session: required service exit terminates the session + cleanup, returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let openChild = null

  const code = await runE2eInteractive({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () => {
      openChild = makeOpenCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        // Simulate a required application service exiting during the session.
        process.kill(state.owned.leader, 'SIGKILL')
      })
      return openChild
    },
  })

  assert.equal(
    code,
    1,
    'service exit must end the session with visible failure'
  )
  assert.ok(
    openChild && openChild.killed,
    'Cypress child must be signalled to stop after the service exit'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('interactive cancel: SIGTERM-ignoring Cypress child is escalated to SIGKILL, then owned tree is cleaned with zero survivors', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let stickyChild = null
  const killSignals = []

  const code = await runE2eInteractive({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    cancelEscalationMs: 50,
    spawnCypress: () => {
      stickyChild = makeStickyCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
        cancel.trigger()
      })
      const realKill = stickyChild.kill
      stickyChild.kill = (signal) => {
        killSignals.push(signal)
        return realKill.call(stickyChild, signal)
      }
      return stickyChild
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.deepEqual(
    killSignals,
    ['SIGTERM', 'SIGKILL'],
    'wrapper must escalate SIGTERM → SIGKILL when the Cypress child ignores SIGTERM'
  )
  assert.ok(stickyChild && stickyChild.killed === 'SIGKILL')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('normal close path is unaffected: no spurious SIGKILL when Cypress exits on its own without cancellation', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const killSignals = []

  const code = await runE2eInteractive({
    argv: [],
    ...isolatedLifetimeOpts(checkout.root, standIn),
    cancelEscalationMs: 50,
    spawnCypress: () => {
      const child = makeOpenCypressChild(async (session) => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        // Developer closes Cypress cleanly — no cancellation triggered.
        session.close()
      })
      const realKill = child.kill
      child.kill = (signal) => {
        killSignals.push(signal)
        return realKill.call(child, signal)
      }
      return child
    },
  })

  assert.equal(code, 0, 'a clean close must return 0 with no cancellation')
  assert.deepEqual(killSignals, [], 'no signal must be sent on a clean close')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})
