import assert from 'node:assert'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  healthyOnce,
  makeLogs,
  makeMockChild,
  neverHealthy,
} from './sut-start-fixtures.mjs'
import { waitForSutHealthy } from './sut-start.mjs'

test('waitForSutHealthy returns ok=true when healthcheck passes immediately', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    const child = makeMockChild()
    const logs = makeLogs()

    const result = await waitForSutHealthy({
      child,
      timeoutMs: 5_000,
      pollMs: 50,
      logFile,
      log: logs.log,
      errLog: logs.errLog,
      healthcheckFn: healthyOnce,
    })

    assert.strictEqual(result.ok, true)
    assert.strictEqual(result.exitCode, 0)
    assert.ok(
      logs.out.some((s) => /healthy/i.test(s)),
      'should log healthy message'
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('waitForSutHealthy returns ok=false when timeout expires', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    const child = makeMockChild()
    const logs = makeLogs()

    const result = await waitForSutHealthy({
      child,
      timeoutMs: 100,
      pollMs: 50,
      logFile,
      log: logs.log,
      errLog: logs.errLog,
      healthcheckFn: neverHealthy,
    })

    assert.strictEqual(result.ok, false)
    assert.strictEqual(result.exitCode, 1)
    assert.ok(
      logs.err.some((s) => /timeout|did not become/i.test(s)),
      'should log timeout error'
    )
    assert.ok(
      logs.err.some((s) => /sut\.log/i.test(s) || /log:/i.test(s)),
      'should mention log file'
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('waitForSutHealthy reports early child exit as failure', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    const child = makeMockChild()
    const logs = makeLogs()

    // Trigger exit immediately via setImmediate so the poll loop sees it
    setImmediate(() => child.emit('exit', 1, null))

    const result = await waitForSutHealthy({
      child,
      timeoutMs: 5_000,
      pollMs: 50,
      logFile,
      log: logs.log,
      errLog: logs.errLog,
      healthcheckFn: neverHealthy,
    })

    assert.strictEqual(result.ok, false)
    assert.strictEqual(result.exitCode, 1)
    assert.ok(
      logs.err.some((s) => /exited|killed/i.test(s)),
      'should report early exit'
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('waitForSutHealthy reports child killed by signal as failure', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    const child = makeMockChild()
    const logs = makeLogs()

    setImmediate(() => child.emit('exit', null, 'SIGKILL'))

    const result = await waitForSutHealthy({
      child,
      timeoutMs: 5_000,
      pollMs: 50,
      logFile,
      log: logs.log,
      errLog: logs.errLog,
      healthcheckFn: neverHealthy,
    })

    assert.strictEqual(result.ok, false)
    assert.strictEqual(result.exitCode, 1)
    assert.ok(
      logs.err.some((s) => /SIGKILL/i.test(s)),
      'should mention the signal name'
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('waitForSutHealthy includes tail of log file in failure output', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    await writeFile(logFile, 'line1\nline2\nSomething went wrong\n', 'utf8')

    const child = makeMockChild()
    const logs = makeLogs()

    const result = await waitForSutHealthy({
      child,
      timeoutMs: 100,
      pollMs: 50,
      logFile,
      log: logs.log,
      errLog: logs.errLog,
      healthcheckFn: neverHealthy,
    })

    assert.strictEqual(result.ok, false)
    assert.ok(
      logs.err.some((s) => /Something went wrong/.test(s)),
      'should include log tail in stderr'
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})
