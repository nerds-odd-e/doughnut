import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makeLinkedWorktreeCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { isPidAlive, writeIsolatedConfig } from './sut-isolated-fixtures.mjs'
import {
  clearSessionDeps,
  runCheck,
  spawnReparentedBackendJvm,
} from './worktree-retirement-test-helpers.mjs'

test('reparented backend JVM with database session refuses and stays alive', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  const pid = await spawnReparentedBackendJvm(t, checkout.root)

  const result = await runCheck(checkout.root, {
    listDatabaseSessionsFn: async () => [
      {
        id: '77',
        user: 'doughnut',
        host: 'localhost',
        db: 'doughnut_wt_a7c2_test',
        command: 'Sleep',
        time: '12',
      },
    ],
  })
  assert.equal(result.code, 1)
  assert.match(result.err, /recorded evidence prevents cleanup/)
  assert.match(
    result.err,
    new RegExp(`surviving checkout backend JVM \\(pid ${pid}\\)`)
  )
  assert.match(result.err, /active database session/)
  assert.equal(result.err.includes('DROP'), false)
  assert.equal(isPidAlive(pid), true)
})

test('disconnected orphan backend JVM without listener or session refuses and stays alive', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  const pid = await spawnReparentedBackendJvm(t, checkout.root)

  const result = await runCheck(checkout.root, clearSessionDeps)
  assert.equal(result.code, 1)
  assert.match(result.err, /surviving checkout backend JVM/)
  assert.match(result.err, new RegExp(`pid ${pid}`))
  assert.equal(result.err.includes('active database session'), false)
  assert.equal(result.err.includes('listener'), false)
  assert.equal(result.err.includes('DROP'), false)
  assert.equal(isPidAlive(pid), true)
})

test('checkout process inspection failure refuses', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root, {
    listDatabaseSessionsFn: async () => [],
    listProcessTableFn: async () => {
      throw new Error('ps enumeration failed')
    },
  })
  assert.equal(result.code, 1)
  assert.match(result.err, /unable to inspect checkout processes/)
  assert.match(result.err, /ps enumeration failed/)
})

test('ambiguous checkout JVM evidence refuses', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root, {
    listDatabaseSessionsFn: async () => [],
    listProcessTableFn: async () => [
      {
        pid: 4242,
        ppid: 1,
        command: `/usr/bin/java -cp . Hold unrelated`,
      },
    ],
    listJavaWorkingDirectoriesFn: async () => new Map([[4242, checkout.root]]),
  })
  assert.equal(result.code, 1)
  assert.match(result.err, /ambiguous checkout JVM evidence/)
  assert.match(result.err, /pid 4242/)
})

test('verified idle fixture reports idle snapshot not a reservation', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root, clearSessionDeps)
  assert.equal(result.code, 0, result.err)
  assert.match(result.out, /idle snapshot/i)
  assert.match(result.out, /not a deletion reservation/i)
  assert.equal(result.out.includes('incomplete'), false)
  assert.equal(result.err.includes('DROP'), false)
})
