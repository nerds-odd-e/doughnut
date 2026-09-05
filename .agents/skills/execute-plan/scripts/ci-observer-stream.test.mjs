import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { readMailboxEvents } from './ci-mailbox.mjs'
import { createObserverStreamParser } from './ci-observer-stream.mjs'
import { waitForFile } from './watch-ci-test-fixtures.mjs'

const fixture = fileURLToPath(
  new URL('./ci-observer-stream-fixture.mjs', import.meta.url)
)

test('observer output parser retains chunk tails and pre-yield events', () => {
  const parser = createObserverStreamParser()
  const mailbox = '/tmp/observer/watch-1'
  const output = [
    `CI_OBSERVER ${JSON.stringify({ directory: mailbox })}\n`,
    `${JSON.stringify({ sequence: 1, event: { type: 'CI_FAILURE', runId: 41 } })}\n`,
    `${JSON.stringify({ sequence: 2, event: { type: 'CI_INCOMPLETE', runId: 42 } })}\n`,
    `CI_OBSERVER_RESULT ${JSON.stringify({ directory: mailbox, terminal: { status: 'stopped' } })}\n`,
  ].join('')
  const receiptEnd = output.indexOf('\n') + 1
  const firstEventEnd = output.indexOf('\n', receiptEnd) + 1
  const boundaries = [
    firstEventEnd,
    firstEventEnd + 7,
    output.length - 3,
    output.length,
  ]
  const deliveries = []
  const terminals = []
  let offset = 0
  let yielded = false
  for (const boundary of boundaries) {
    const parsed = parser.push(output.slice(offset, boundary))
    deliveries.push(...parsed.events.map((event) => ({ event, yielded })))
    terminals.push(...parsed.terminals)
    offset = boundary
    yielded = true
  }

  assert.deepEqual(
    deliveries.map(({ event }) => event),
    [
      { type: 'CI_FAILURE', runId: 41 },
      { type: 'CI_INCOMPLETE', runId: 42 },
    ]
  )
  assert.equal(deliveries[0].yielded, false)
  assert.deepEqual(terminals, [
    { directory: mailbox, terminal: { status: 'stopped' } },
  ])
  assert.equal(parser.tail(), '')
})

test('foreground mailbox stream delivers successive real-observer records before exit', async (t) => {
  const state = mkdtempSync(join(tmpdir(), 'ci-codex-stream-test-'))
  t.after(() => rmSync(state, { recursive: true, force: true }))
  const child = spawn(process.execPath, [fixture, state], {
    env: { ...process.env, DONUT_CI_MAILBOX_ROOT: state, TMPDIR: state },
  })
  t.after(() => child.kill('SIGTERM'))
  const parser = createObserverStreamParser()
  const directories = []
  const events = []
  child.stdout.on('data', (chunk) => {
    const parsed = parser.push(chunk.toString())
    directories.push(...parsed.directories)
    events.push(...parsed.events)
  })

  await waitForFile(join(state, 'first-failure-recorded'))
  assert.equal(events.length, 1)
  assert.match(events[0].failedJobs[0].name, /backend failure/)
  assert.equal(child.exitCode, null)

  writeFileSync(join(state, 'release-second-failure'), '')
  await new Promise((resolve, reject) => {
    child.once('exit', (code) =>
      code === 0 ? resolve() : reject(new Error(`fixture exited ${code}`))
    )
  })

  assert.equal(directories.length, 1)
  assert.deepEqual(
    events.map(({ failedJobs }) => failedJobs[0].name),
    [
      'labelled fake-GitHub backend failure',
      'labelled fake-GitHub frontend timeout',
    ]
  )
  assert.deepEqual(
    readMailboxEvents(directories[0]).map(({ event }) => event),
    events
  )
  assert.equal(parser.tail(), '')
  assert.deepEqual(
    JSON.parse(readFileSync(join(directories[0], 'result.json'))),
    { status: 'finished' }
  )
})
