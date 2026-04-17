import assert from 'node:assert'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { EventEmitter } from 'node:events'
import { test } from 'node:test'
import {
  spawnSutServices,
  waitForSutHealthy,
  writePidFile,
  runSutStart,
} from './sut-start.mjs'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Build a mock child process emitter with controllable exit. */
function makeMockChild(pid = 99999) {
  const child = new EventEmitter()
  child.pid = pid
  child.unref = () => undefined
  return child
}

/** Spy that captures calls to log / errLog. */
function makeLogs() {
  const out = []
  const err = []
  return {
    log: (s) => out.push(s),
    errLog: (s) => err.push(s),
    out,
    err,
  }
}

/** A healthcheck function that returns ok=true immediately. */
async function healthyOnce() {
  return {
    ok: true,
    tcpResults: [],
    readinessResult: { ok: true },
    exitCode: 0,
  }
}

/** A healthcheck function that always returns ok=false. */
async function neverHealthy() {
  return {
    ok: false,
    tcpResults: [],
    readinessResult: { ok: false },
    exitCode: 1,
  }
}

// ---------------------------------------------------------------------------
// writePidFile
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// spawnSutServices
// ---------------------------------------------------------------------------

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
    assert.strictEqual(spawnCalls[0].cmd, 'pnpm')
    assert.deepStrictEqual(spawnCalls[0].args, [
      'exec',
      'run-p',
      '-clnr',
      'backend:sut',
      'start:mb',
      'local:lb:vite',
      'frontend:sut',
    ])
    assert.strictEqual(spawnCalls[0].opts.detached, true)
    assert.strictEqual(spawnCalls[0].opts.shell, false)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

// ---------------------------------------------------------------------------
// waitForSutHealthy
// ---------------------------------------------------------------------------

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
    // Write some fake log content
    const { writeFile } = await import('node:fs/promises')
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

// ---------------------------------------------------------------------------
// runSutStart (integration of spawn + poll)
// ---------------------------------------------------------------------------

test('runSutStart exits 0 when services become healthy', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-start-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    const pidFile = path.join(dir, 'sut.pid')
    const mockChild = makeMockChild(777)
    const logs = makeLogs()

    const code = await runSutStart({
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
