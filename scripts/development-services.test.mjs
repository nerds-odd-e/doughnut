import assert from 'node:assert/strict'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import {
  DEVELOPMENT_SERVICE_ARGS,
  runDevelopmentServices,
} from './development-services.mjs'
import { makeMockChild } from './sut-start-fixtures.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

test('DEVELOPMENT_SERVICE_ARGS are reload-capable and never include Mountebank', () => {
  assert.deepEqual(DEVELOPMENT_SERVICE_ARGS, [
    'exec',
    'run-p',
    '-lnr',
    'backend:dev',
    'local:lb:vite',
    'frontend:dev',
  ])
  assert.equal(DEVELOPMENT_SERVICE_ARGS.includes('start:mb'), false)
  assert.equal(DEVELOPMENT_SERVICE_ARGS.includes('backend:sut'), false)
  assert.equal(DEVELOPMENT_SERVICE_ARGS.includes('frontend:sut'), false)
})

test('runDevelopmentServices spawns Development group with target env and log', () => {
  const mockChild = makeMockChild()
  const spawnCalls = []
  const writes = []
  const logWriter = {
    write: (chunk) => writes.push(String(chunk)),
    close: () => undefined,
  }

  const child = runDevelopmentServices({
    spawnFn: (cmd, args, opts) => {
      spawnCalls.push({ cmd, args, opts })
      return mockChild
    },
    logWriter,
  })

  child.stdout.emit('data', Buffer.from('dev stdout\n'))
  child.stderr.emit('data', Buffer.from('dev stderr\n'))

  assert.equal(child, mockChild)
  assert.equal(spawnCalls.length, 1)
  assert.equal(spawnCalls[0].cmd, 'pnpm')
  assert.deepEqual(spawnCalls[0].args, DEVELOPMENT_SERVICE_ARGS)
  assert.equal(spawnCalls[0].opts.shell, false)
  assert.equal(spawnCalls[0].opts.detached, true)
  assert.equal(spawnCalls[0].opts.cwd, repoRoot)
  assert.equal(spawnCalls[0].opts.env.SERVER_PORT, '8081')
  assert.equal(spawnCalls[0].opts.env.LOCAL_LB_LISTEN_PORT, '5175')
  assert.equal(
    spawnCalls[0].opts.env.LOCAL_LB_VITE_UPSTREAM,
    'http://127.0.0.1:5176'
  )
  assert.equal(spawnCalls[0].opts.env.LOCAL_LB_BACKEND, 'http://127.0.0.1:8081')
  assert.equal(spawnCalls[0].opts.env.FRONTEND_DEV_PORT, '5176')
  assert.equal(spawnCalls[0].opts.env.SUT_RUNTIME_TARGET, undefined)
  assert.equal(spawnCalls[0].opts.env.SUT_OWNER_TOKEN, undefined)
  assert.equal(
    DEVELOPMENT_RUNTIME_TARGET.logFile,
    path.join(repoRoot, 'dev.log')
  )
  assert.deepEqual(writes, ['dev stdout\n', 'dev stderr\n'])
})
