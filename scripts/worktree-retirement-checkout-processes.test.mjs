import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makeLinkedWorktreeCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { isPidAlive, writeIsolatedConfig } from './sut-isolated-fixtures.mjs'
import { listJavaWorkingDirectories } from './worktree-retirement-checkout-processes.mjs'
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

test('supported JVM with unavailable cwd via lsof exit 1 refuses check', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root, {
    listDatabaseSessionsFn: async () => [],
    listProcessTableFn: async () => [
      {
        pid: 5151,
        ppid: 1,
        command: `/usr/bin/java -cp . Hold com.odde.donut.DonutApplication`,
      },
    ],
    listJavaWorkingDirectoriesFn: (pids) =>
      listJavaWorkingDirectories(pids, {
        execFileFn: async () => {
          const error = new Error('lsof incomplete')
          error.code = 1
          error.stdout = ''
          throw error
        },
      }),
  })
  assert.equal(result.code, 1)
  assert.match(result.err, /recorded evidence prevents cleanup/)
  assert.match(result.err, /uncertain checkout backend JVM evidence/)
  assert.match(result.err, /pid 5151/)
  assert.match(result.err, /working directory unavailable/)
  assert.equal(result.err.includes('DROP'), false)
})

test('lsof exit 1 with partial output omits missing pids from the cwd map', async () => {
  const cwdByPid = await listJavaWorkingDirectories([10, 20], {
    execFileFn: async () => {
      const error = new Error('lsof partial')
      error.code = 1
      error.stdout = 'p10\nn/peer/other\n'
      throw error
    },
  })
  assert.equal(cwdByPid.get(10), '/peer/other')
  assert.equal(cwdByPid.has(20), false)
})

test('supported JVM with positively unrelated peer cwd stays idle eligible', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root, {
    listDatabaseSessionsFn: async () => [],
    listProcessTableFn: async () => [
      {
        pid: 6161,
        ppid: 1,
        command: `/usr/bin/java -cp . Hold com.odde.donut.DonutApplication`,
      },
    ],
    listJavaWorkingDirectoriesFn: async () =>
      new Map([[6161, '/tmp/peer-checkout-unrelated']]),
  })
  assert.equal(result.code, 0, result.err)
  assert.match(result.out, /idle snapshot/i)
  assert.equal(result.err.includes('uncertain'), false)
  assert.equal(result.err.includes('surviving'), false)
})

test('non-supported JVM with unavailable cwd does not invent a relation veto', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root, {
    listDatabaseSessionsFn: async () => [],
    listProcessTableFn: async () => [
      {
        pid: 8181,
        ppid: 1,
        command: `/usr/bin/java -cp . Hold unrelated`,
      },
    ],
    listJavaWorkingDirectoriesFn: async () => new Map(),
  })
  assert.equal(result.code, 0, result.err)
  assert.match(result.out, /idle snapshot/i)
  assert.equal(result.err.includes('uncertain'), false)
  assert.equal(result.err.includes('ambiguous'), false)
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
