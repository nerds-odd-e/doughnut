import assert from 'node:assert/strict'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import {
  browserOrigin,
  healthEndpoints,
  runtimeTargetProcessEnv,
} from './local-runtime-target.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

test('Development runtime fixes profile, database, ports, and artifact paths', () => {
  assert.equal(DEVELOPMENT_RUNTIME_TARGET.profile, 'dev')
  assert.equal(DEVELOPMENT_RUNTIME_TARGET.database, 'doughnut_development')
  assert.equal(DEVELOPMENT_RUNTIME_TARGET.backendPort, 8081)
  assert.equal(DEVELOPMENT_RUNTIME_TARGET.lbListenPort, 5175)
  assert.equal(DEVELOPMENT_RUNTIME_TARGET.vitePort, 5176)
  assert.equal(DEVELOPMENT_RUNTIME_TARGET.mountebankPort, undefined)
  assert.equal(
    DEVELOPMENT_RUNTIME_TARGET.logFile,
    path.join(repoRoot, 'dev.log')
  )
  assert.equal(
    DEVELOPMENT_RUNTIME_TARGET.pidFile,
    path.join(repoRoot, 'dev.pid')
  )
  assert.equal(
    browserOrigin(DEVELOPMENT_RUNTIME_TARGET),
    'http://127.0.0.1:5175'
  )

  const health = healthEndpoints(DEVELOPMENT_RUNTIME_TARGET)
  assert.deepEqual(
    health.tcpChecks.map((check) => [check.service, check.port]),
    [
      ['backend', 8081],
      ['local LB', 5175],
      ['frontend vite', 5176],
    ]
  )
  assert.equal(health.readinessUrl, 'http://127.0.0.1:5175/__lb__/ready')

  const env = runtimeTargetProcessEnv(DEVELOPMENT_RUNTIME_TARGET)
  assert.equal(env.SERVER_PORT, '8081')
  assert.equal(env.LOCAL_LB_LISTEN_PORT, '5175')
  assert.equal(env.FRONTEND_DEV_PORT, '5176')
  assert.equal(env.SUT_RUNTIME_TARGET, undefined)
})
