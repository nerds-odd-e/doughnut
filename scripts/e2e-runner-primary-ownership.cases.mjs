import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnForeignProcess,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
} from './sut-isolated-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import { startOwnedSutLifetime } from './sut-start.mjs'
import {
  makeCypressChild,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  trackOwnedTree,
  primaryLifetimeOpts,
} from './e2e-runner-lifetime-fixtures.mjs'

test('primary batch: unconfigured target → canonical endpoints → SUT owned by invocation → Cypress runs → cleanup → zero surviving owned processes', async (t) => {
  const checkout = makePrimaryCheckout(t)
  // No writeIsolatedConfig: this is an unconfigured primary checkout.
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
      }),
  })

  assert.equal(code, 0)
  // Same lifecycle as the isolated target: the owned supervisor child +
  // grandchild are reaped via the shared owned-tree termination.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Primary target claims no SUT ownership (no owner lock dir is created).
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('primary batch: shared MySQL/Redis and Development preserved — a foreign peer survives, no SUT owner lock is created', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
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
    ...primaryLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
      }),
  })

  assert.equal(code, 0)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Shared infrastructure / foreign peers are not signalled by the batch.
  assert.equal(isPidAlive(foreign.pid), true, 'foreign peer must survive')
  // No SUT ownership is claimed on the primary target.
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

test('primary batch: lifetime shutdown failure still stops the owned process tree and preserves Cypress status', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const errors = []
  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    errLog: (message) => errors.push(message),
    startLifetime: async (options) => {
      const lifetime = await startOwnedSutLifetime(options)
      lifetime.shutdown = async () => {
        throw new Error('primary lifetime shutdown failed')
      }
      return lifetime
    },
    spawnCypress: () =>
      makeCypressChild(3, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }),
  })

  assert.match(errors.join('\n'), /primary lifetime shutdown failed/)
  assert.equal(code, 3, 'must preserve the nonzero Cypress exit code')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})
