import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { runSutHealthcheck } from './sut-healthcheck.mjs'
import { runSutServices } from './sut-services.mjs'
import { healthyOnce, makeMockChild } from './sut-start-fixtures.mjs'
import { runSutStart } from './sut-start.mjs'

const explicitRuntimeTarget = {
  backendPort: 19081,
  vitePort: 15174,
  lbListenPort: 15173,
  mountebankPort: 12525,
  databaseUrl:
    'jdbc:mysql://127.0.0.1:3309/doughnut_e2e_wt_fixture?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true',
}

function assertTargetProcessEnv(env) {
  assert.equal(env.SERVER_PORT, '19081')
  assert.equal(env.INPUT_DB_URL, explicitRuntimeTarget.databaseUrl)
  assert.equal(env.LOCAL_LB_BACKEND, 'http://127.0.0.1:19081')
  assert.equal(env.LOCAL_LB_VITE_UPSTREAM, 'http://127.0.0.1:15174')
  assert.equal(env.LOCAL_LB_LISTEN_PORT, '15173')
  assert.equal(env.FRONTEND_DEV_PORT, '15174')
  assert.equal(env.FRONTEND_BACKEND_ORIGIN, 'http://127.0.0.1:19081')
  assert.deepEqual(JSON.parse(env.SUT_RUNTIME_TARGET), explicitRuntimeTarget)
}

function makeServiceChild() {
  const child = new EventEmitter()
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.killed = false
  child.kill = () => undefined
  return child
}

test('launcher feeds one explicit runtime target through services, start, and readiness', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'sut-runtime-target-'))
  try {
    const serviceSpawnCalls = []
    runSutServices({
      runtimeTarget: explicitRuntimeTarget,
      spawnFn: (cmd, args, opts) => {
        serviceSpawnCalls.push({ cmd, args, opts })
        return makeServiceChild()
      },
      logWriter: { write: () => undefined, close: () => undefined },
    })
    assert.equal(serviceSpawnCalls.length, 1)
    assertTargetProcessEnv(serviceSpawnCalls[0].opts.env)

    const startSpawnCalls = []
    const healthcheckCalls = []
    const code = await runSutStart({
      checkoutRoot: dir,
      runtimeTarget: explicitRuntimeTarget,
      spawnFn: (cmd, args, opts) => {
        startSpawnCalls.push({ cmd, args, opts })
        return makeMockChild(4242)
      },
      logFile: path.join(dir, 'sut.log'),
      pidFile: path.join(dir, 'sut.pid'),
      timeoutMs: 5_000,
      pollMs: 50,
      log: () => undefined,
      errLog: () => undefined,
      healthcheckFn: async (opts) => {
        healthcheckCalls.push(opts)
        return healthyOnce()
      },
    })
    assert.equal(code, 0)
    assert.equal(startSpawnCalls.length, 1)
    assertTargetProcessEnv(startSpawnCalls[0].opts.env)
    assert.equal(
      startSpawnCalls[0].opts.env.SUT_LOG_FILE,
      path.join(dir, 'sut.log')
    )
    assert.ok(healthcheckCalls.length > 0)
    assert.deepEqual(healthcheckCalls[0].runtimeTarget, explicitRuntimeTarget)

    const healthLogs = []
    const health = await runSutHealthcheck({
      checkoutRoot: dir,
      runtimeTarget: explicitRuntimeTarget,
      log: (line) => healthLogs.push(line),
    })
    assert.equal(health.ok, false)
    assert.deepEqual(
      health.tcpResults.map((result) => [result.service, result.port]),
      [
        ['mountebank', 12525],
        ['backend', 19081],
        ['local LB', 15173],
        ['frontend vite', 15174],
      ]
    )
    assert.equal(
      health.readinessResult.url,
      'http://127.0.0.1:15173/__lb__/ready'
    )
    assert.match(
      healthLogs.join('\n'),
      /local LB not listening on 127\.0\.0\.1:15173/
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})
