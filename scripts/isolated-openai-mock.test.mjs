import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  SHARED_MOUNTEBANK_MANAGEMENT_PORT,
  SHARED_OPEN_AI_SERVING_PORT,
  startPrivateOpenAiMock,
} from './isolated-openai-mock.mjs'
import { getListenerPids } from './sut-listener-pids.mjs'

test('startPrivateOpenAiMock owns management and serving listeners then stops cleanly', async (t) => {
  const mock = await startPrivateOpenAiMock({
    allocation: {
      e2e: { backendPort: 19081, vitePort: 15174, lbListenPort: 15173 },
    },
  })
  t.after(() => mock.stop())

  assert.notEqual(
    new URL(mock.endpoint.managementUrl).port,
    String(SHARED_MOUNTEBANK_MANAGEMENT_PORT)
  )
  assert.notEqual(mock.endpoint.servingPort, SHARED_OPEN_AI_SERVING_PORT)
  assert.notEqual(mock.endpoint.servingPort, 19081)
  await mock.verifyOwnership()

  const managementPort = Number(new URL(mock.endpoint.managementUrl).port)
  assert.ok((await getListenerPids(managementPort)).length > 0)
  assert.ok((await getListenerPids(mock.endpoint.servingPort)).length > 0)

  await mock.stop()
  assert.equal((await getListenerPids(managementPort)).length, 0)
  assert.equal((await getListenerPids(mock.endpoint.servingPort)).length, 0)
})

test('startPrivateOpenAiMock refuses when the mock child exits before ownership', async () => {
  const fakeChild = {
    pid: 9_000_001,
    exitCode: 1,
    signalCode: null,
    unref() {
      /* stub */
    },
    once(event, fn) {
      if (event === 'exit') queueMicrotask(() => fn(1, null))
    },
    removeListener() {
      /* stub */
    },
    kill() {
      /* stub */
    },
  }

  await assert.rejects(
    () =>
      startPrivateOpenAiMock({
        allocation: {
          e2e: { backendPort: 19081, vitePort: 15174, lbListenPort: 15173 },
        },
        spawnFn: () => fakeChild,
      }),
    /exited before management port|Refusing bind\/start failure/
  )
})
