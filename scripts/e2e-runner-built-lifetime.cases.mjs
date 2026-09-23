import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'

import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
} from './sut-isolated-fixtures.mjs'
import { SUPPORTED_ISOLATED_CYPRESS_SPEC } from './isolated-cypress-spec-selection.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import { runE2eBatch } from './e2e-runner.mjs'

import {
  makeCypressChild,
  makeLiveCypressChild,
  manualCancel,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  trackOwnedTree,
  primaryLifetimeOpts,
  BUILT_RUNTIME_TARGET,
} from './e2e-runner-lifetime-fixtures.mjs'

test('built-asset batch: target arguments select the built target → SUT starts without Vite → readiness → Cypress runs → cleanup → zero survivors', async (t) => {
  const checkout = makePrimaryCheckout(t)
  // No writeIsolatedConfig: this is an unconfigured primary checkout with
  // prepared bundles (built-asset target).
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let capturedHealthcheckTarget = null
  let capturedSpecs = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn, {
      // Capture the runtimeTarget that reaches the healthcheck — it must
      // carry built: true, proving the built target flows through the
      // existing resolveSutCheckoutTarget → startOwnedSutLifetime →
      // waitForSutHealthy path. Readiness does NOT wait for a Vite listener
      // (the built target has no Vite dev server).
      healthcheckFn: async (hcOpts) => {
        capturedHealthcheckTarget = hcOpts.runtimeTarget
        return {
          ok: true,
          tcpResults: [],
          readinessResult: { ok: true },
          exitCode: 0,
        }
      },
    }),
    runtimeTarget: BUILT_RUNTIME_TARGET,
    spawnCypress: (opts) => {
      capturedSpecs = opts.specs
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
      })
    },
  })

  assert.equal(code, 0)
  // Target arguments forwarded correctly: the built target reaches the
  // healthcheck with built: true (distinguished from dev/isolated/primary).
  assert.equal(
    capturedHealthcheckTarget?.built,
    true,
    'built target forwarded to healthcheck'
  )
  // Selection forwarding: selected specs reach Cypress.
  assert.ok(
    capturedSpecs && capturedSpecs.length === 1,
    'one selected spec forwarded to Cypress'
  )
  assert.equal(
    capturedSpecs[0],
    SUPPORTED_ISOLATED_CYPRESS_SPEC,
    'the selected supported spec is forwarded to Cypress'
  )
  // Cleanup: zero surviving owned processes.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('built-asset batch: nonzero Cypress result still settles the owned tree (same lifecycle)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    runtimeTarget: BUILT_RUNTIME_TARGET,
    spawnCypress: () =>
      makeCypressChild(7, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }),
  })

  assert.equal(code, 7, 'must preserve the nonzero Cypress exit code')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('built-asset batch: cancellation stops Cypress + owned tree, returns nonzero, zero survivors', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let cypressChild = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    runtimeTarget: BUILT_RUNTIME_TARGET,
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        cancel.trigger()
      })
      return cypressChild
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('built-asset batch: required service exit terminates Cypress + settles owned tree, returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressChild = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    runtimeTarget: BUILT_RUNTIME_TARGET,
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        // Simulate a required application service exiting during the run.
        process.kill(state.owned.leader, 'SIGKILL')
      })
      return cypressChild
    },
  })

  assert.equal(code, 1, 'service exit must end the run with visible failure')
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})
