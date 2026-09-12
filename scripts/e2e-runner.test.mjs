import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnForeignProcess,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { SUPPORTED_ISOLATED_CYPRESS_SPEC } from './isolated-cypress-spec-selection.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import { healthyOnce, neverHealthy } from './sut-start-fixtures.mjs'
import { runE2eBatch } from './e2e-runner.mjs'

function makeCypressChild(exitCode, onSpawn) {
  const child = new EventEmitter()
  child.pid = 0
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.kill = () => undefined
  child.unref = () => undefined
  // emit exit on next tick so listeners attach first; onSpawn runs first so
  // tests can observe the live owned tree before shutdown.
  queueMicrotask(async () => {
    try {
      await onSpawn?.()
    } catch {
      // observation best-effort; do not block the exit
    }
    child.emit('exit', exitCode, null)
  })
  return child
}

function makeCypressLaunchError(error, onSpawn) {
  return () => {
    const child = new EventEmitter()
    child.pid = 0
    child.kill = () => undefined
    child.unref = () => undefined
    queueMicrotask(async () => {
      try {
        await onSpawn?.()
      } catch {
        // observation best-effort
      }
      child.emit('error', error)
    })
    return child
  }
}

function isolatedLifetimeOpts(checkoutRoot, standIn, extra = {}) {
  return {
    checkoutRoot,
    spawnFn: standIn.spawnFn,
    logFile: path.join(checkoutRoot, 'sut.log'),
    pidFile: path.join(checkoutRoot, 'sut.pid'),
    timeoutMs: extra.timeoutMs ?? 5_000,
    pollMs: extra.pollMs ?? 50,
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: extra.healthcheckFn ?? healthyOnce,
    databaseExistsFn: () => true,
    portClaimRoot: path.join(checkoutRoot, '.doughnut-e2e-port-claims'),
    isPortOccupiedFn: async () => false,
  }
}

function cypressArgv(spec = SUPPORTED_ISOLATED_CYPRESS_SPEC) {
  return ['--spec', spec]
}

function trackOwnedTree(t, getOwned) {
  t.after(() => {
    const owned = getOwned()
    if (Number.isInteger(owned?.leader) && owned.leader > 0) {
      try {
        process.kill(-owned.leader, 'SIGKILL')
      } catch {
        // already gone
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

test('batch lifetime: start → run → shutdown returns Cypress outcome', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
      }),
  })

  assert.equal(code, 0)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('failed startup: readiness failure cleans partial owned work and returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressSpawned = false

  // Capture owned pids while the tree is alive (before the readiness timeout
  // settles and shutdown reaps it).
  const pidsPromise = waitForOwnedPids(standIn.pidsFile)
  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: neverHealthy,
      timeoutMs: 1_500,
    }),
    spawnCypress: () => {
      cypressSpawned = true
      return makeCypressChild(0)
    },
  })
  state.owned = await pidsPromise

  assert.equal(code, 1)
  assert.equal(
    cypressSpawned,
    false,
    'Cypress must not run after readiness failure'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('failed Cypress launch: cleans owned work and returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: makeCypressLaunchError(
      Object.assign(new Error('spawn EACCES'), { code: 'EACCES' }),
      async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }
    ),
  })

  assert.equal(code, 1)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('nonzero test result: cleans owned work and returns the nonzero result', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(7, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }),
  })

  assert.equal(code, 7, 'must preserve Cypress nonzero exit code')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('peer survival: a foreign process is not signalled by the batch', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
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
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }),
  })

  assert.equal(code, 0)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(isPidAlive(foreign.pid), true, 'foreign peer must survive')
})

test('unsupported spec selection refuses before the stack starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  let startCalled = false

  const code = await runE2eBatch({
    argv: [
      '--spec',
      'e2e_test/features/note_creation_and_update/note_creation.feature',
    ],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1)
  assert.equal(startCalled, false, 'must refuse before starting the stack')
})

test('private-mock spec refuses in slice 3 before the stack starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  let startCalled = false

  const code = await runE2eBatch({
    argv: [
      '--spec',
      'e2e_test/features/ai_generated_content/note_content_completion.feature',
    ],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1)
  assert.equal(startCalled, false)
})

test('missing explicit --spec refuses before the stack starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  let startCalled = false

  const code = await runE2eBatch({
    argv: [],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1)
  assert.equal(startCalled, false)
})
