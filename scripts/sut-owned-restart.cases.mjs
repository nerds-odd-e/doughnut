import assert from 'node:assert/strict'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnForeignProcess,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { startOwnedSutLifetime } from './sut-owned-lifetime.mjs'
import { readOwnerRecord, restartSutApplication } from './sut-owner-control.mjs'
import {
  acquireSutRunnerLease,
  beginSutOwnerShutdown,
  releaseSutRunnerLease,
  verifyLiveSutOwner,
} from './sut-owner.mjs'
import {
  spawnDetachedOwnedSupervisor,
  sutPeerNames,
  readPeerPids,
  waitForPeerPids,
  waitUntil,
  writeOwnedSupervisorRunPPeers,
} from './sut-owned-supervisor-fixtures.mjs'

test('owned lifetime restart requires its active lease and replaces only its application tree', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  writeOwnedSupervisorRunPPeers(checkout.root)
  const foreign = spawnForeignProcess()
  t.after(() => foreign.kill('SIGKILL'))
  let fixture
  const lifetime = await startOwnedSutLifetime({
    checkoutRoot: checkout.root,
    logFile: path.join(checkout.root, 'sut.log'),
    pidFile: path.join(checkout.root, 'sut.pid'),
    schemaExistsFn: () => true,
    databaseExistsFn: () => true,
    isPortOccupiedFn: async () => false,
    log: () => undefined,
    healthcheckFn: async () => ({
      ok: (await verifyLiveSutOwner(checkout.root)).ok,
    }),
    spawnFn: (_command, _args, options) => {
      fixture = spawnDetachedOwnedSupervisor(t, {
        checkoutRoot: checkout.root,
        owner: {
          token: options.env.SUT_OWNER_TOKEN,
          controlPath: options.env.SUT_OWNER_CONTROL_PATH,
        },
        logFile: options.env.SUT_LOG_FILE,
      })
      return fixture.supervisor
    },
    pollMs: 20,
    timeoutMs: 5000,
  })
  t.after(() => lifetime.shutdown())
  assert.equal((await lifetime.ready).ok, true)
  fixture.state.pids = await waitForPeerPids(checkout.root)
  const before = await verifyLiveSutOwner(checkout.root)
  const oldPids = fixture.state.pids
  await assert.rejects(
    lifetime.restartApplication(),
    /matching active runner lease/
  )
  const lease = await acquireSutRunnerLease(checkout.root)
  try {
    await assert.rejects(
      lifetime.restartApplication('another-runner'),
      /matching active runner lease/
    )
    await assert.rejects(
      beginSutOwnerShutdown(checkout.root),
      /Cypress runner is using/
    )
    const owner = await readOwnerRecord(checkout.root)
    await assert.rejects(
      restartSutApplication(checkout.root, lease, {
        ...owner,
        token: 'another-owner',
      }),
      /restart failed/
    )
    assert.equal(
      (await verifyLiveSutOwner(checkout.root)).applicationGroupId,
      before.applicationGroupId
    )
    assert.ok(Object.values(oldPids).every(isPidAlive))
    const restarted = await lifetime.restartApplication(lease)
    assert.notEqual(restarted.applicationGroupId, before.applicationGroupId)
    fixture.state.pids = await waitUntil(
      async () => {
        const pids = await readPeerPids(checkout.root)
        return sutPeerNames.every(
          (name) => pids[name] !== oldPids[name] && isPidAlive(pids[name])
        )
          ? pids
          : false
      },
      5000,
      'replacement application peers did not start'
    )
    assert.ok(Object.values(oldPids).every((pid) => !isPidAlive(pid)))
    const after = await verifyLiveSutOwner(checkout.root)
    assert.equal(after.pid, before.pid)
    assert.equal(after.runnerLeaseHeld, true)
  } finally {
    await releaseSutRunnerLease(checkout.root, lease)
  }
  await lifetime.shutdown()
  assert.ok(Object.values(fixture.state.pids).every((pid) => !isPidAlive(pid)))
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
  assert.equal(isPidAlive(foreign.pid), true)
})
