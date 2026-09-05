import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  checkoutRoot,
  createMailbox,
  publishMailboxEvent,
  readDeliveryProgress,
} from './ci-mailbox.mjs'
import {
  failure,
  input,
  interruptHostHookWhileWriting,
  runHostHook,
  setup,
} from './ci-host-hook-test-fixtures.mjs'

test('Cursor redelivers an event when the hook process is interrupted before output completes', async () => {
  const options = setup(checkoutRoot)
  const directory = createMailbox({}, options)
  await runHostHook('cursor', input('cursor', directory), options)
  publishMailboxEvent(directory, {
    ...failure,
    diagnostic: 'x'.repeat(2 * 1024 * 1024),
  })

  await interruptHostHookWhileWriting('cursor', input('cursor'), options)

  assert.deepEqual(readDeliveryProgress(directory), { deliveredThrough: 0 })
  assert.deepEqual(
    await runHostHook(
      'cursor',
      input('cursor', undefined, { generation_id: 'child-request' }),
      options
    ),
    {}
  )
  assert.deepEqual(
    await runHostHook(
      'cursor',
      input('cursor', undefined, { conversation_id: 'other' }),
      options
    ),
    {}
  )
  assert.deepEqual(readDeliveryProgress(directory), { deliveredThrough: 0 })

  const delivered = await runHostHook('cursor', input('cursor'), options)
  assert.deepEqual(Object.keys(delivered), ['additional_context'])
  assert.match(delivered.additional_context, /CI_FAILURE/)
  assert.deepEqual(readDeliveryProgress(directory), { deliveredThrough: 1 })
  assert.deepEqual(await runHostHook('cursor', input('cursor'), options), {})
})
