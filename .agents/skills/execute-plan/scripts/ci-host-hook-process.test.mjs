import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  checkoutRoot,
  createMailbox,
  publishMailboxEvent,
  readDeliveryProgress,
} from './ci-mailbox.mjs'
import {
  context,
  failure,
  input,
  interruptHostHookWhileWriting,
  runHostHook,
  setup,
} from './ci-host-hook-test-fixtures.mjs'

async function proveInterruptedDelivery({
  host,
  nonOwningInputs,
  assertOutputShape,
}) {
  const options = setup(checkoutRoot)
  const directory = createMailbox({}, options)
  await runHostHook(host, input(host, directory), options)
  publishMailboxEvent(directory, {
    ...failure,
    diagnostic: 'x'.repeat(2 * 1024 * 1024),
  })

  await interruptHostHookWhileWriting(host, input(host), options)

  assert.deepEqual(readDeliveryProgress(directory), { deliveredThrough: 0 })
  for (const overrides of nonOwningInputs)
    assert.deepEqual(
      await runHostHook(host, input(host, undefined, overrides), options),
      {}
    )
  assert.deepEqual(readDeliveryProgress(directory), { deliveredThrough: 0 })

  const delivered = await runHostHook(host, input(host), options)
  assertOutputShape(delivered)
  assert.deepEqual(readDeliveryProgress(directory), { deliveredThrough: 1 })
  assert.deepEqual(await runHostHook(host, input(host), options), {})
}

test('Cursor redelivers an event when the hook process is interrupted before output completes', async () => {
  await proveInterruptedDelivery({
    host: 'cursor',
    nonOwningInputs: [
      { generation_id: 'child-request' },
      { conversation_id: 'other' },
    ],
    assertOutputShape(delivered) {
      assert.deepEqual(Object.keys(delivered), ['additional_context'])
      assert.match(delivered.additional_context, /CI_FAILURE/)
    },
  })
})

test('Claude Code redelivers an event when the hook process is interrupted before output completes', async () => {
  await proveInterruptedDelivery({
    host: 'claude',
    nonOwningInputs: [{ agent_id: 'child-request' }, { session_id: 'other' }],
    assertOutputShape(delivered) {
      assert.deepEqual(Object.keys(delivered), ['hookSpecificOutput'])
      assert.equal(delivered.hookSpecificOutput.hookEventName, 'PostToolUse')
      assert.match(context(delivered), /CI_FAILURE/)
    },
  })
})
