import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makeLinkedWorktreeCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  lockPaths,
  writeStaleOwnerLock,
} from './backend-test-worktree-lock-fixtures.mjs'
import {
  allocateFreePort,
  closeServer,
  isPidAlive,
  isTcpListening,
  listenTcp,
  spawnForeignProcess,
  startLiveOwner,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  acquireSutRunnerLease,
  claimSutOwnership,
  sutOwnerLockDir,
  verifyLiveSutOwner,
} from './sut-owner.mjs'
import { runCheck } from './worktree-retirement-test-helpers.mjs'

function writeLiveBackendOwnerLock(t, checkout) {
  const child = spawnForeignProcess()
  t.after(() => {
    try {
      child.kill()
    } catch {
      // already gone
    }
  })
  const { dir, ownerFile } = lockPaths(checkout)
  mkdirSync(dir)
  writeFileSync(ownerFile, String(child.pid))
  return child
}

test('busy backend worktree owner refuses and leaves the fixture alive', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  const child = writeLiveBackendOwnerLock(t, checkout)

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /recorded evidence prevents cleanup/)
  assert.match(result.err, /busy backend worktree owner/)
  assert.equal(result.err.includes('DROP'), false)
  assert.equal(isPidAlive(child.pid), true)
  assert.equal(existsSync(lockPaths(checkout).dir), true)
})

test('stale backend worktree owner record refuses without reclaiming', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  const stalePid = writeStaleOwnerLock(checkout)

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /stale or unverifiable backend worktree owner/)
  assert.match(result.err, /not reclaimed/)
  assert.equal(existsSync(lockPaths(checkout).dir), true)
  assert.equal(isPidAlive(stalePid), false)
})

test('live SUT owner refuses and leaves the owner control alive', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /busy live SUT owner/)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, true)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), true)
})

test('Cypress runner lease refuses while the owner remains', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  await acquireSutRunnerLease(checkout.root)

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /busy live SUT owner/)
  assert.match(result.err, /busy Cypress runner lease/)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, true)
  assert.equal((await verifyLiveSutOwner(checkout.root)).runnerLeaseHeld, true)
})

test('stale unverifiable SUT owner record refuses without reclaiming', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  await claimSutOwnership(checkout.root)
  const dead = spawnSync(process.execPath, ['-e', ''])
  writeFileSync(
    path.join(sutOwnerLockDir(checkout.root), 'starting.pid'),
    String(dead.pid)
  )

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /stale or unverifiable SUT owner/)
  assert.match(result.err, /not reclaimed/)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), true)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('foreign recorded listener refuses and leaves the listener alive', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const vitePort = await allocateFreePort()
  const lbListenPort = await allocateFreePort()
  const listener = await listenTcp()
  t.after(() => closeServer(listener.server))
  writeIsolatedConfig(checkout.root, {
    id: 'wt_a7c2',
    e2e: {
      database: 'doughnut_e2e_wt_a7c2',
      backendPort: listener.port,
      vitePort,
      lbListenPort,
    },
  })

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /foreign or unverified listener/)
  assert.match(result.err, new RegExp(`port ${listener.port}`))
  assert.equal(result.err.includes('DROP'), false)
  assert.equal(await isTcpListening(listener.port), true)
})

test('active database sessions refuse without DROP', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root, {
    listDatabaseSessionsFn: async () => [
      {
        id: '42',
        user: 'doughnut',
        host: 'localhost',
        db: 'doughnut_wt_a7c2_test',
        command: 'Query',
        time: '3',
      },
    ],
  })
  assert.equal(result.code, 1)
  assert.match(result.err, /active database session/)
  assert.match(result.err, /doughnut_wt_a7c2_test/)
  assert.equal(result.err.includes('DROP'), false)
})

test('database session inspection failure refuses', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root, {
    listDatabaseSessionsFn: async () => {
      throw new Error('mysql administration failed')
    },
  })
  assert.equal(result.code, 1)
  assert.match(result.err, /unable to inspect database sessions/)
  assert.match(result.err, /mysql administration failed/)
})
