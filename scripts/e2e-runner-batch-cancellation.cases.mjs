import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnForeignProcess,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
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
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

test('cancel during readiness-wait: stops owned descendants and returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let cypressSpawned = false

  // Trigger cancellation only once the owned tree has published its pids, so
  // the test observes a live tree before the readiness wait is aborted.
  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: async () => {
        if (existsSync(standIn.pidsFile)) {
          if (state.owned.leader === 0) {
            state.owned = JSON.parse(readFileSync(standIn.pidsFile, 'utf8'))
            cancel.trigger()
          }
        }
        return {
          ok: false,
          tcpResults: [],
          readinessResult: { ok: false },
          exitCode: 1,
        }
      },
      timeoutMs: 30_000,
    }),
    spawnCypress: () => {
      cypressSpawned = true
      return makeCypressChild(0)
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.equal(
    cypressSpawned,
    false,
    'Cypress must not run after readiness cancel'
  )
  assert.ok(state.owned.leader > 0, 'owned tree was alive when cancelled')
  // Completion waits for bounded escalation: the owned tree is actually gone,
  // not merely signalled.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Ownership is released only after the owned tree is stopped.
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('cancel during Cypress run: stops Cypress child and owned descendants, returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let cypressChild = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
        // Trigger cancellation while the Cypress child is running.
        cancel.trigger()
      })
      return cypressChild
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  // The Cypress child (the runner) was stopped first by the cancellation.
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop'
  )
  // Completion waits for bounded escalation: owned tree actually gone.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Ownership released only after the owned tree is stopped.
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('cancel during Cypress run: a foreign peer survives while the owned leader+grandchild are reaped', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  const foreign = spawnForeignProcess()
  t.after(() => {
    try {
      foreign.kill('SIGKILL')
    } catch {
      // already gone
    }
  })

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        cancel.trigger()
      }),
    cancel,
  })

  assert.equal(code, 1)
  // Representative child/grandchild exit: owned leader and grandchild reaped.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Peer survival: the foreign process is NOT signalled by the batch.
  assert.equal(
    isPidAlive(foreign.pid),
    true,
    'foreign peer must survive cancellation'
  )
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})
