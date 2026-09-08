import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { test } from 'node:test'
import {
  descendantPidsByParentWalk,
  isOwnedByApplicationTree,
  parentPid,
  processGroupId,
} from './sut-listener-pids.mjs'

test('isOwnedByApplicationTree accepts a matching process group', async () => {
  assert.equal(
    await isOwnedByApplicationTree(42, 7, {
      processGroupIdFn: async (pid) => (pid === 42 ? 7 : undefined),
      parentPidFn: async () => {
        throw new Error('should not walk when PGID matches')
      },
    }),
    true
  )
})

test('isOwnedByApplicationTree accepts an ancestor in the application group', async () => {
  const pgids = new Map([
    [10, 3],
    [20, 7],
  ])
  const ppids = new Map([
    [10, 20],
    [20, 1],
  ])
  assert.equal(
    await isOwnedByApplicationTree(10, 7, {
      processGroupIdFn: async (pid) => pgids.get(pid),
      parentPidFn: async (pid) => ppids.get(pid),
    }),
    true
  )
})

test('isOwnedByApplicationTree accepts an ancestor whose PID is the application group', async () => {
  const pgids = new Map([
    [10, 3],
    [99, 99],
  ])
  const ppids = new Map([
    [10, 99],
    [99, 1],
  ])
  assert.equal(
    await isOwnedByApplicationTree(10, 99, {
      processGroupIdFn: async (pid) => pgids.get(pid),
      parentPidFn: async (pid) => ppids.get(pid),
    }),
    true
  )
})

test('isOwnedByApplicationTree rejects a foreign process with no ancestry', async () => {
  const pgids = new Map([
    [10, 3],
    [20, 4],
  ])
  const ppids = new Map([
    [10, 20],
    [20, 1],
  ])
  assert.equal(
    await isOwnedByApplicationTree(10, 99, {
      processGroupIdFn: async (pid) => pgids.get(pid),
      parentPidFn: async (pid) => ppids.get(pid),
    }),
    false
  )
})

test('parentPid and descendant walk see a live child', async (t) => {
  const child = spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], {
    stdio: 'ignore',
  })
  t.after(() => {
    try {
      child.kill('SIGKILL')
    } catch {
      // already gone
    }
  })
  assert.ok(child.pid > 0)
  assert.equal(await parentPid(child.pid), process.pid)
  const descendants = await descendantPidsByParentWalk(process.pid)
  assert.ok(
    descendants.includes(child.pid),
    `expected ${child.pid} in descendants ${descendants.join(',')}`
  )
  assert.equal(
    await processGroupId(child.pid),
    await processGroupId(process.pid)
  )
})
