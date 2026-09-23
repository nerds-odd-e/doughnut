import assert from 'node:assert/strict'

import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  allocateFreePort,
  closeServer,
  listenTcp,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import {
  makeCypressChild,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  isPortStillListening,
  readyLifetimeStandIn,
} from './e2e-runner-lifetime-fixtures.mjs'

test('fresh isolated worktree (no .worktree.local.json) provisions on first cy:run: startLifetime is called instead of failing in resolveSutCheckoutTarget', async (t) => {
  const checkout = makePrimaryCheckout(t)
  // No writeIsolatedConfig: simulate a fresh linked worktree with no
  // allocation. isIsolatedCheckoutFn fakes the linked-worktree topology so
  // the wrapper treats this checkout as isolated without a real git worktree.
  let startCalled = false
  const code = await runE2eBatch({
    argv: cypressArgv(),
    checkoutRoot: checkout.root,
    isIsolatedCheckoutFn: () => true,
    startLifetime: async () => {
      startCalled = true
      return readyLifetimeStandIn()
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 0)
  assert.equal(
    startCalled,
    true,
    'fresh isolated worktree must provision (startLifetime called); the wrapper must not fail in resolveSutCheckoutTarget before provisioning'
  )
})

test('primary checkout regression guard: foreign listener on a canonical port still refuses BEFORE startLifetime (resolveSutCheckoutTarget + port check run first)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const foreignListener = await listenTcp()
  t.after(() => closeServer(foreignListener.server))
  const freeBackend = await allocateFreePort()
  const freeVite = await allocateFreePort()
  const runtimeTarget = {
    backendPort: freeBackend,
    vitePort: freeVite,
    lbListenPort: foreignListener.port,
    mountebankPort: 2525,
  }
  let startCalled = false

  const code = await runE2eBatch({
    argv: cypressArgv(),
    checkoutRoot: checkout.root,
    runtimeTarget,
    isIsolatedCheckoutFn: () => false,
    isPortOccupiedFn: async (port) => port === foreignListener.port,
    startLifetime: async () => {
      startCalled = true
      throw new Error(
        'must not start when a canonical port is foreign-occupied'
      )
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1, 'must refuse when a canonical port is foreign-occupied')
  assert.equal(
    startCalled,
    false,
    'primary path must call resolveSutCheckoutTarget + port check before startLifetime'
  )
  assert.equal(
    await isPortStillListening(foreignListener.port),
    true,
    'foreign listener must not be signalled'
  )
})

test('already-provisioned isolated worktree still works: startLifetime is called and re-reads the existing allocation (no re-provisioning path skipped)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  let startCalled = false
  const code = await runE2eBatch({
    argv: cypressArgv(),
    checkoutRoot: checkout.root,
    isIsolatedCheckoutFn: () => true,
    startLifetime: async () => {
      startCalled = true
      return readyLifetimeStandIn()
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 0)
  assert.equal(
    startCalled,
    true,
    'already-provisioned isolated worktree must still reach startLifetime'
  )
})
