import assert from 'node:assert/strict'
import { existsSync, mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { test } from 'node:test'
import {
  checkoutRoot,
  createMailbox,
  mailboxRoot,
  publishMailboxEvent,
  readMailbox,
  readMailboxEvents,
  requestMailboxStop,
  runMailboxWorker,
} from './ci-mailbox.mjs'

function createTestMailbox(t, request = {}) {
  const storage = mkdtempSync(join(tmpdir(), 'ci-mailbox-test-'))
  t.after(() => rmSync(storage, { recursive: true, force: true }))
  const options = { root: '/test/donut', storage }
  return { directory: createMailbox(request, options), options }
}

test('a mailbox under a process TMPDIR is outside the shared observer directory', (t) => {
  const nixTmp = mkdtempSync(join(tmpdir(), 'nix-shell-'))
  t.after(() => rmSync(nixTmp, { recursive: true, force: true }))
  const directory = createMailbox(
    { probe: true },
    { root: checkoutRoot, storage: nixTmp }
  )
  assert.notEqual(resolve(directory, '..'), resolve(mailboxRoot))
  assert.throws(
    () => readMailbox(directory),
    /CI mailbox is outside the observer directory/
  )
})

test('event evidence is appendable and independent of worker status', (t) => {
  const { directory } = createTestMailbox(t)
  const failure = { type: 'CI_FAILURE', runId: 42, attempt: 1 }
  publishMailboxEvent(directory, failure)
  publishMailboxEvent(directory, { type: 'CI_INCOMPLETE', runId: 43 })
  assert.deepEqual(readMailboxEvents(directory), [
    { sequence: 1, event: failure },
    { sequence: 2, event: { type: 'CI_INCOMPLETE', runId: 43 } },
  ])
  assert.equal(existsSync(join(directory, 'result.json')), false)
})

test('stopping an active watcher records no failure event', async (t) => {
  const { directory, options } = createTestMailbox(t, { mode: 'execution' })
  let started
  const ready = new Promise((resolve) => {
    started = resolve
  })
  const running = runMailboxWorker(directory, {
    ...options,
    observe: ({ signal }) =>
      new Promise((resolve, reject) => {
        signal.addEventListener('abort', () => reject(signal.reason), {
          once: true,
        })
        started()
      }),
  })
  await ready
  requestMailboxStop(directory, options)
  await running
  assert.deepEqual(JSON.parse(readFileSync(join(directory, 'result.json'))), {
    status: 'stopped',
    coverage: { state: 'ended', pendingCi: 'unobserved' },
    evidence: { recordedThrough: 0, deliveredThrough: 0, unread: 0 },
  })
  assert.deepEqual(readMailboxEvents(directory), [])
})

test('a stop requested before worker startup does not start observation', async (t) => {
  const { directory, options } = createTestMailbox(t, { mode: 'execution' })
  requestMailboxStop(directory, options)
  await runMailboxWorker(directory, {
    ...options,
    observe: async () => assert.fail('observation should not start'),
  })
  assert.deepEqual(JSON.parse(readFileSync(join(directory, 'result.json'))), {
    status: 'stopped',
    coverage: { state: 'ended', pendingCi: 'unobserved' },
    evidence: { recordedThrough: 0, deliveredThrough: 0, unread: 0 },
  })
})

test('a worker error records monitoring unavailability', async (t) => {
  const { directory, options } = createTestMailbox(t)
  await runMailboxWorker(directory, {
    ...options,
    observe: async () => {
      throw new Error('broken observer')
    },
  })
  assert.deepEqual(JSON.parse(readFileSync(join(directory, 'result.json'))), {
    status: 'finished',
  })
  assert.equal(
    readMailboxEvents(directory)[0].event.type,
    'CI_MONITOR_UNAVAILABLE'
  )
})
