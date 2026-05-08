import assert from 'node:assert'
import { EventEmitter } from 'node:events'
import { test } from 'node:test'
import { runSutServices, SUT_SERVICE_ARGS } from './sut-services.mjs'

function makeMockChild() {
  const child = new EventEmitter()
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.killed = false
  child.kill = (signal) => {
    child.killed = signal
  }
  return child
}

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
  assert.deepStrictEqual(writes, ['stdout line\n', 'stderr line\n'])
})
