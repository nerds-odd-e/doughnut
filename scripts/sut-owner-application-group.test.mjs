import assert from 'node:assert/strict'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  startLiveOwner,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { processGroupId } from './sut-listener-pids.mjs'
import {
  spawnDetachedOwnedSupervisor,
  sutPeerNames,
  waitForPeerPids,
  waitUntil,
  writeOwnedSupervisorRunPPeers,
} from './sut-owned-supervisor-fixtures.mjs'
import {
  claimSutOwnership,
  releaseSutOwnership,
  verifyLiveSutOwner,
} from './sut-owner.mjs'

test('live owner control exposes the spawned application process group', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  writeOwnedSupervisorRunPPeers(checkout.root)
  const owner = await claimSutOwnership(checkout.root)
  t.after(async () => {
    await releaseSutOwnership(checkout.root)
  })
  const { state } = spawnDetachedOwnedSupervisor(t, {
    checkoutRoot: checkout.root,
    owner,
    logFile: path.join(checkout.root, 'sut.log'),
  })

  const live = await waitUntil(
    async () => {
      const result = await verifyLiveSutOwner(checkout.root)
      return result.ok ? result : false
    },
    5_000,
    'timed out waiting for live SUT owner'
  )
  state.pids = await waitForPeerPids(checkout.root)

  const owned = await verifyLiveSutOwner(checkout.root)
  const peerGroupIds = await Promise.all(
    sutPeerNames.map((name) => processGroupId(state.pids[name]))
  )
  assert.equal(owned.ok, true)
  assert.equal(
    typeof owned.applicationGroupId,
    'number',
    'owned supervisor must publish the live application group'
  )
  assert.ok(owned.applicationGroupId > 0)
  assert.notEqual(owned.applicationGroupId, live.pid)
  assert.ok(
    peerGroupIds.every((pgid) => pgid === owned.applicationGroupId),
    `application group ${owned.applicationGroupId} must match peer groups ${peerGroupIds.join(',')}`
  )
})

test('control-only live owner supplies no application group', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  await startLiveOwner(checkout.root, t)
  const owned = await verifyLiveSutOwner(checkout.root)
  assert.equal(owned.ok, true)
  assert.equal(owned.applicationGroupId, undefined)
})
