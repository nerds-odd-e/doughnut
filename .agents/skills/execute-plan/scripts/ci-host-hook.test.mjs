import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { deliverCiEvents } from './ci-host-hook.mjs'
import {
  createMailbox,
  probeMailbox,
  receiptPrefix,
  runMailboxWorker,
  stopMailbox,
} from './ci-mailbox.mjs'

function setup() {
  const storage = mkdtempSync(join(tmpdir(), 'ci-hook-test-'))
  return { root: '/test/donut', storage }
}
const failure = { type: 'CI_FAILURE', runId: 42, attempt: 1 }
const input = (host, directory, overrides = {}) => ({
  session_id: 'main',
  conversation_id: 'main',
  generation_id: 'coordinator-turn',
  transcript_path: '/test/main.jsonl',
  hook_event_name: host === 'cursor' ? 'postToolUse' : 'PostToolUse',
  tool_name: host === 'cursor' ? 'Shell' : 'Bash',
  tool_output: JSON.stringify({
    stdout: directory
      ? `${receiptPrefix}${JSON.stringify({ directory })}\n`
      : '',
  }),
  tool_response: {
    stdout: directory
      ? `${receiptPrefix}${JSON.stringify({ directory })}\n`
      : '',
  },
  ...overrides,
})
const context = (output) =>
  output.additional_context ?? output.hookSpecificOutput?.additionalContext

for (const host of ['cursor', 'claude']) {
  test(`${host}: real probe receipt produces a host-native readiness notification`, () => {
    const options = setup()
    const directory = probeMailbox(options)
    if (host === 'claude')
      assert.deepEqual(
        deliverCiEvents(
          input(host, directory, { cursor_version: 'test' }),
          host,
          options
        ),
        {}
      )
    const output = deliverCiEvents(input(host, directory), host, options)
    assert.match(context(output), /CI_MONITOR_READY/)
    assert.deepEqual(deliverCiEvents(input(host, directory), host, options), {})
  })

  test(`${host}: pending work stays quiet and a completed failure arrives exactly once`, async () => {
    const options = setup()
    const directory = createMailbox({}, options)
    deliverCiEvents(input(host, directory), host, options)
    assert.deepEqual(deliverCiEvents(input(host), host, options), {})
    await runMailboxWorker(directory, {
      ...options,
      observe: async () => failure,
    })
    assert.match(
      context(deliverCiEvents(input(host), host, options)),
      /CI_FAILURE/
    )
    assert.deepEqual(deliverCiEvents(input(host), host, options), {})
  })

  test(`${host}: a successful observation adds no model context`, async () => {
    const options = setup()
    const directory = createMailbox({}, options)
    deliverCiEvents(input(host, directory), host, options)
    await runMailboxWorker(directory, { ...options, observe: async () => null })
    assert.deepEqual(deliverCiEvents(input(host), host, options), {})
  })

  test(`${host}: another task or worker cannot consume the coordinator's notification`, async () => {
    const options = setup()
    const directory = createMailbox({}, options)
    deliverCiEvents(input(host, directory), host, options)
    await runMailboxWorker(directory, {
      ...options,
      observe: async () => failure,
    })
    for (const overrides of [
      { conversation_id: 'other', session_id: 'other' },
      { agent_id: 'worker', subagent_id: 'worker' },
    ])
      assert.deepEqual(
        deliverCiEvents(input(host, directory, overrides), host, options),
        {}
      )
    assert.match(
      context(deliverCiEvents(input(host), host, options)),
      /CI_FAILURE/
    )
  })

  test(`${host}: a failure already available at stop is delivered without waiting for CI`, async () => {
    const options = setup()
    const directory = createMailbox({}, options)
    deliverCiEvents(input(host, directory), host, options)
    await runMailboxWorker(directory, {
      ...options,
      observe: async () => failure,
    })
    const output = deliverCiEvents(
      input(host, undefined, {
        hook_event_name: host === 'cursor' ? 'stop' : 'Stop',
      }),
      host,
      options
    )
    assert.match(output.followup_message ?? output.reason, /CI_FAILURE/)
  })
}

test('Cursor child with the same conversation and transcript cannot consume the coordinator event', async () => {
  const options = setup()
  const directory = createMailbox({}, options)
  deliverCiEvents(input('cursor', directory), 'cursor', options)
  await runMailboxWorker(directory, {
    ...options,
    observe: async () => failure,
  })
  for (const generation_id of ['child-request', undefined]) {
    assert.deepEqual(
      deliverCiEvents(
        input('cursor', directory, { generation_id }),
        'cursor',
        options
      ),
      {}
    )
  }
  assert.match(
    context(deliverCiEvents(input('cursor'), 'cursor', options)),
    /CI_FAILURE/
  )
})

test('Cursor reattaches after a real user prompt, not an arbitrary child tool call', async () => {
  const options = setup()
  const directory = createMailbox({}, options)
  deliverCiEvents(input('cursor', directory), 'cursor', options)
  await runMailboxWorker(directory, {
    ...options,
    observe: async () => failure,
  })
  const nextTurn = { generation_id: 'next-user-message' }
  assert.deepEqual(
    deliverCiEvents(input('cursor', undefined, nextTurn), 'cursor', options),
    {}
  )
  assert.deepEqual(
    deliverCiEvents(
      input('cursor', undefined, {
        ...nextTurn,
        hook_event_name: 'beforeSubmitPrompt',
      }),
      'cursor',
      options
    ),
    {}
  )
  assert.match(
    context(
      deliverCiEvents(input('cursor', undefined, nextTurn), 'cursor', options)
    ),
    /CI_FAILURE/
  )
})

test('Cursor cancellation does not auto-continue or consume a waiting failure', async () => {
  const options = setup()
  const directory = createMailbox({}, options)
  deliverCiEvents(input('cursor', directory), 'cursor', options)
  await runMailboxWorker(directory, {
    ...options,
    observe: async () => failure,
  })
  assert.deepEqual(
    deliverCiEvents(
      input('cursor', undefined, {
        hook_event_name: 'stop',
        status: 'aborted',
      }),
      'cursor',
      options
    ),
    {}
  )
  assert.match(
    context(deliverCiEvents(input('cursor'), 'cursor', options)),
    /CI_FAILURE/
  )
})

test('stopping an active watcher aborts its work and produces no failure notification', async () => {
  const options = setup()
  const directory = createMailbox({}, options)
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
  stopMailbox(directory, options)
  await running
  assert.deepEqual(JSON.parse(readFileSync(join(directory, 'result.json'))), {
    status: 'stopped',
  })
})

test('a worker process error becomes a monitoring-unavailable event', async () => {
  const options = setup()
  const directory = createMailbox({}, options)
  await runMailboxWorker(directory, {
    ...options,
    observe: async () => {
      throw new Error('broken observer')
    },
  })
  assert.equal(
    JSON.parse(readFileSync(join(directory, 'result.json'))).event.type,
    'CI_MONITOR_UNAVAILABLE'
  )
})
