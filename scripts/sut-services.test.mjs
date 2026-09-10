import assert from 'node:assert'
import { test } from 'node:test'
import { runSutServices, SUT_SERVICE_ARGS } from './sut-services.mjs'
import { makeMockChild } from './sut-start-fixtures.mjs'

test('runSutServices starts run-p and writes stdout/stderr to the rotating writer', () => {
  const mockChild = makeMockChild()
  const spawnCalls = []
  const writes = []
  const logWriter = {
    write: (chunk) => writes.push(String(chunk)),
    close: () => undefined,
  }

  const child = runSutServices({
    spawnFn: (cmd, args, opts) => {
      spawnCalls.push({ cmd, args, opts })
      return mockChild
    },
    logWriter,
  })

  child.stdout.emit('data', Buffer.from('stdout line\n'))
  child.stderr.emit('data', Buffer.from('stderr line\n'))

  assert.strictEqual(child, mockChild)
  assert.strictEqual(spawnCalls[0].cmd, 'pnpm')
  assert.deepStrictEqual(spawnCalls[0].args, SUT_SERVICE_ARGS)
  assert.strictEqual(spawnCalls[0].opts.shell, false)
  assert.strictEqual(
    spawnCalls[0].opts.env.LOCAL_LB_VITE_UPSTREAM,
    'http://127.0.0.1:5174'
  )
  assert.strictEqual(spawnCalls[0].opts.env.SERVER_PORT, '9081')
  assert.strictEqual(spawnCalls[0].opts.env.LOCAL_LB_LISTEN_PORT, '5173')
  assert.strictEqual(
    spawnCalls[0].opts.env.LOCAL_LB_BACKEND,
    'http://127.0.0.1:9081'
  )
  assert.deepStrictEqual(writes, ['stdout line\n', 'stderr line\n'])
})
