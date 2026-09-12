import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { runSutHealthcheck } from './sut-healthcheck.mjs'
import {
  closeServer,
  isPidAlive,
  isTcpListening,
  listenTcp,
  runConfiguredStart,
  spawnForeignProcess,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import { healthyOnce, neverHealthy } from './sut-start-fixtures.mjs'
import { startOwnedSutLifetime } from './sut-start.mjs'

function trackOwnedTree(t, getOwned) {
  t.after(() => {
    const owned = getOwned()
    if (Number.isInteger(owned?.leader) && owned.leader > 0) {
      try {
        process.kill(-owned.leader, 'SIGKILL')
      } catch {
        // already gone or not a group leader
      }
    }
    for (const pid of [owned?.leader, owned?.grandchild]) {
      if (!Number.isInteger(pid) || pid <= 0) continue
      try {
        process.kill(pid, 'SIGKILL')
      } catch {
        // already gone
      }
    }
  })
}

function prepareOwnedStandIn(t, extra = {}) {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root, extra.env ?? {})
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const start = runConfiguredStart(checkout.root, standIn, {
    healthcheckFn: neverHealthy,
    timeoutMs: extra.timeoutMs ?? 5_000,
    signal: extra.signal,
    errLog: extra.errLog,
  })
  return { checkout, standIn, start, state }
}

test('isolated start timeout stops the owned process tree and releases the claim', async (t) => {
  const foreign = spawnForeignProcess()
  const foreignListener = await listenTcp()
  t.after(() => {
    try {
      foreign.kill('SIGKILL')
    } catch {
      // already gone
    }
    closeServer(foreignListener.server)
  })
  const { checkout, standIn, start, state } = prepareOwnedStandIn(t, {
    timeoutMs: 1_500,
  })
  state.owned = await waitForOwnedPids(standIn.pidsFile)
  assert.equal(isPidAlive(state.owned.leader), true)
  assert.equal(isPidAlive(state.owned.grandchild), true)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), true)

  const code = await start
  assert.equal(code, 1)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(isPidAlive(foreign.pid), true)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
  const health = await runSutHealthcheck({
    checkoutRoot: checkout.root,
    log: () => undefined,
  })
  assert.equal(health.ok, false)
  assert.equal(await isTcpListening(foreignListener.port), true)
})

test('isolated start early exit reaps leftover owned grandchildren', async (t) => {
  const { standIn, start, state } = prepareOwnedStandIn(t, {
    env: { SUT_STANDIN_EXIT: '1' },
  })
  state.owned = await waitForOwnedPids(standIn.pidsFile)
  const code = await start
  assert.equal(code, 1)
  assert.equal(isPidAlive(state.owned.grandchild), false)
})

test('isolated start escalates past a TERM-resistant owned descendant', async (t) => {
  const foreign = spawnForeignProcess()
  t.after(() => {
    try {
      foreign.kill('SIGKILL')
    } catch {
      // already gone
    }
  })
  const { standIn, start, state } = prepareOwnedStandIn(t, {
    env: { SUT_STANDIN_GRANDCHILD_IGNORE_TERM: '1' },
    timeoutMs: 100,
  })
  state.owned = await waitForOwnedPids(standIn.pidsFile)

  const code = await start
  assert.equal(code, 1)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(isPidAlive(foreign.pid), true)
})

test('isolated start cancellation stops the owned process tree', async (t) => {
  const controller = new AbortController()
  const errors = []
  const { standIn, start, state } = prepareOwnedStandIn(t, {
    timeoutMs: 30_000,
    signal: controller.signal,
    errLog: (line) => errors.push(line),
  })
  state.owned = await waitForOwnedPids(standIn.pidsFile)
  controller.abort()
  const code = await start
  assert.equal(code, 1)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.ok(errors.some((line) => /cancelled/i.test(line)))
})

test('startOwnedSutLifetime shutdown stops a healthy owned stack and releases ownership', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const lifetime = await startOwnedSutLifetime({
    checkoutRoot: checkout.root,
    spawnFn: standIn.spawnFn,
    logFile: path.join(checkout.root, 'sut.log'),
    pidFile: path.join(checkout.root, 'sut.pid'),
    timeoutMs: 5_000,
    pollMs: 50,
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: healthyOnce,
    databaseExistsFn: () => true,
    portClaimRoot: path.join(checkout.root, '.doughnut-e2e-port-claims'),
    isPortOccupiedFn: async () => false,
  })

  state.owned = await waitForOwnedPids(standIn.pidsFile)
  assert.equal(isPidAlive(state.owned.leader), true)
  assert.equal(isPidAlive(state.owned.grandchild), true)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), true)

  const result = await lifetime.ready
  assert.equal(result.ok, true)
  assert.equal(result.exitCode, 0)

  // The wrapper owns shutdown even after a successful ready: the legacy adapter
  // would leave the supervisor running, but the lifetime caller can settle it.
  await lifetime.shutdown()

  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})
