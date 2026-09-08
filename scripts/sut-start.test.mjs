import assert from 'node:assert'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  healthyOnce,
  makeLogs,
  makeMockChild,
  neverHealthy,
} from './sut-start-fixtures.mjs'
import { runSutStart, spawnSutServices, writePidFile } from './sut-start.mjs'

test('writePidFile writes the PID to the given path', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const pidFile = path.join(dir, 'sut.pid')
    await writePidFile(12345, { pidFile })
    const content = await readFile(pidFile, 'utf8')
    assert.strictEqual(content, '12345')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('spawnSutServices spawns a detached child and returns child + logFile', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    const spawnCalls = []
    const mockChild = makeMockChild(555)

    const fakeSPawn = (cmd, args, opts) => {
      spawnCalls.push({ cmd, args, opts })
      return mockChild
    }

    const result = spawnSutServices({ spawnFn: fakeSPawn, logFile })

    assert.strictEqual(result.child, mockChild)
    assert.strictEqual(result.logFile, logFile)
    assert.strictEqual(spawnCalls.length, 1)
    assert.strictEqual(spawnCalls[0].cmd, process.execPath)
    assert.deepStrictEqual(spawnCalls[0].args, [
      path.resolve('scripts/sut-services.mjs'),
    ])
    assert.strictEqual(spawnCalls[0].opts.detached, true)
    assert.strictEqual(spawnCalls[0].opts.env.SUT_LOG_FILE, logFile)
    assert.strictEqual(spawnCalls[0].opts.stdio, 'ignore')
    assert.strictEqual(spawnCalls[0].opts.shell, false)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runSutStart exits 0 when services become healthy', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    const pidFile = path.join(dir, 'sut.pid')
    const mockChild = makeMockChild(777)
    const logs = makeLogs()

    const code = await runSutStart({
      checkoutRoot: dir,
      spawnFn: () => mockChild,
      logFile,
      pidFile,
      timeoutMs: 5_000,
      pollMs: 50,
      log: logs.log,
      errLog: logs.errLog,
      healthcheckFn: healthyOnce,
    })

    assert.strictEqual(code, 0)
    const pidContent = await readFile(pidFile, 'utf8')
    assert.strictEqual(pidContent, '777')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runSutStart exits 1 when healthcheck times out', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    const pidFile = path.join(dir, 'sut.pid')
    const mockChild = makeMockChild(888)
    const logs = makeLogs()

    const code = await runSutStart({
      checkoutRoot: dir,
      spawnFn: () => mockChild,
      logFile,
      pidFile,
      timeoutMs: 100,
      pollMs: 50,
      log: logs.log,
      errLog: logs.errLog,
      healthcheckFn: neverHealthy,
    })

    assert.strictEqual(code, 1)
    assert.ok(logs.err.length > 0, 'should have error output')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})
