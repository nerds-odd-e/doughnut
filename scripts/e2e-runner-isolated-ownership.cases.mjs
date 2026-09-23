import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  nestOverlongCheckoutLocalSocketRoot,
  spawnOwnedTreeStandIn,
  startLiveOwner,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
  SUPPORTED_ISOLATED_MCP_SPEC,
} from './isolated-cypress-spec-selection.mjs'
import {
  ownerRecordPath,
  SUT_OWNER_SOCKET_NAME,
  sutOwnerLockDir,
  verifyLiveSutOwner,
} from './sut-owner.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import {
  makeCypressChild,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  isolatedLifetimeOpts,
  healthcheckWaitingForLiveOwner,
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

test('supported multi-feature batch shares one owned stack', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(
      `${SUPPORTED_ISOLATED_CYPRESS_SPEC},${SUPPORTED_ISOLATED_MCP_SPEC}`
    ),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: ({ specs }) => {
      assert.deepEqual(specs, [
        SUPPORTED_ISOLATED_CYPRESS_SPEC,
        SUPPORTED_ISOLATED_MCP_SPEC,
      ])
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
      })
    },
  })

  assert.equal(code, 0)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('long-path isolated checkout reaches live owner readiness without a socket override', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const root = nestOverlongCheckoutLocalSocketRoot(checkout.root, t)
  assert.ok(
    Buffer.byteLength(path.join(sutOwnerLockDir(root), SUT_OWNER_SOCKET_NAME)) >
      104
  )
  writeIsolatedConfig(root)
  const standIn = spawnOwnedTreeStandIn(root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(root, standIn, {
      healthcheckFn: healthcheckWaitingForLiveOwner(root),
    }),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal((await verifyLiveSutOwner(root)).ok, true)
        const owner = JSON.parse(readFileSync(ownerRecordPath(root), 'utf8'))
        assert.notEqual(
          owner.controlPath,
          path.join(sutOwnerLockDir(root), SUT_OWNER_SOCKET_NAME)
        )
      }),
  })

  assert.equal(code, 0)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal((await verifyLiveSutOwner(root)).ok, false)
  assert.equal(existsSync(sutOwnerLockDir(root)), false)
})

test('owners at distinct checkout depths stay independent when one settles', async (t) => {
  const peerCheckout = makePrimaryCheckout(t)
  const peerRoot = nestOverlongCheckoutLocalSocketRoot(peerCheckout.root, t)
  writeIsolatedConfig(peerRoot)
  const peer = await startLiveOwner(peerRoot, t)

  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForLiveOwner(checkout.root),
    }),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal((await verifyLiveSutOwner(checkout.root)).ok, true)
        assert.equal((await verifyLiveSutOwner(peerRoot)).ok, true)
      }),
  })

  assert.equal(code, 0)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
  assert.equal((await verifyLiveSutOwner(peerRoot)).ok, true)
  assert.equal(existsSync(peer.owner.controlPath), true)
})
