import assert from 'node:assert/strict'
import { test } from 'node:test'
import { composeCypressPluginEvents } from './composeCypressPluginEvents.mjs'

function lastWriteWinsOn() {
  const listeners = {}
  return {
    listeners,
    on(event, handler) {
      listeners[event] = handler
    },
  }
}

test('later after:spec and after:run registrations still run earlier cleanup', async () => {
  const { listeners, on } = lastWriteWinsOn()
  const composed = composeCypressPluginEvents(on)
  const ran = []

  composed('after:spec', () => {
    ran.push('cleanup-spec')
  })
  composed('after:run', () => {
    ran.push('cleanup-run')
  })
  composed('after:spec', () => {
    ran.push('later-spec')
  })
  composed('after:run', () => {
    ran.push('later-run')
  })

  await listeners['after:spec']()
  await listeners['after:run']()

  assert.deepEqual(ran, [
    'cleanup-spec',
    'later-spec',
    'cleanup-run',
    'later-run',
  ])
})

test('composed completion handlers await each async registration once', async () => {
  const { listeners, on } = lastWriteWinsOn()
  const composed = composeCypressPluginEvents(on)
  let laterSpecResolved = false
  let cleanupSpecCalls = 0
  let laterSpecCalls = 0
  let cleanupRunCalls = 0
  let laterRunCalls = 0

  composed('after:spec', async () => {
    cleanupSpecCalls += 1
  })
  composed('after:spec', async () => {
    laterSpecCalls += 1
    await new Promise((resolve) => setTimeout(resolve, 20))
    laterSpecResolved = true
  })
  composed('after:run', async () => {
    cleanupRunCalls += 1
  })
  composed('after:run', async () => {
    laterRunCalls += 1
    await new Promise((resolve) => setTimeout(resolve, 20))
  })

  await listeners['after:spec']()
  assert.equal(laterSpecResolved, true)
  assert.equal(cleanupSpecCalls, 1)
  assert.equal(laterSpecCalls, 1)

  await listeners['after:run']()
  assert.equal(cleanupRunCalls, 1)
  assert.equal(laterRunCalls, 1)
})
