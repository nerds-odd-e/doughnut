import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  browserOrigin,
  healthEndpoints,
  runtimeTargetProcessEnv,
  withRuntimeTargetEnv,
} from './local-runtime-target.mjs'
import {
  isolatedBrowserOrigin,
  LEGACY_SUT_RUNTIME_TARGET,
  sutHealthEndpoints,
  sutRuntimeTargetProcessEnv,
  withSutRuntimeTargetEnv,
} from './sut-runtime-target.mjs'

test('extracted helpers preserve every existing SUT legacy value', () => {
  const env = runtimeTargetProcessEnv(LEGACY_SUT_RUNTIME_TARGET)
  assert.equal(env.SERVER_PORT, '9081')
  assert.equal(env.LOCAL_LB_BACKEND, 'http://127.0.0.1:9081')
  assert.equal(env.LOCAL_LB_VITE_UPSTREAM, 'http://127.0.0.1:5174')
  assert.equal(env.LOCAL_LB_LISTEN_PORT, '5173')
  assert.equal(env.FRONTEND_DEV_PORT, '5174')
  assert.equal(env.FRONTEND_BACKEND_ORIGIN, 'http://127.0.0.1:9081')
  assert.equal(env.INPUT_DB_URL, undefined)
  assert.equal(env.SUT_RUNTIME_TARGET, undefined)

  assert.deepEqual(healthEndpoints(LEGACY_SUT_RUNTIME_TARGET), {
    tcpChecks: [
      { service: 'mountebank', host: '127.0.0.1', port: 2525 },
      { service: 'backend', host: '127.0.0.1', port: 9081 },
      { service: 'local LB', host: '127.0.0.1', port: 5173 },
      { service: 'frontend vite', host: '127.0.0.1', port: 5174 },
    ],
    readinessUrl: 'http://127.0.0.1:5173/__lb__/ready',
  })
  assert.equal(
    browserOrigin(LEGACY_SUT_RUNTIME_TARGET),
    'http://127.0.0.1:5173'
  )
})

test('SUT adapter decorates the shared process env with SUT_RUNTIME_TARGET', () => {
  const shared = runtimeTargetProcessEnv(LEGACY_SUT_RUNTIME_TARGET)
  const sut = sutRuntimeTargetProcessEnv(LEGACY_SUT_RUNTIME_TARGET)
  assert.deepEqual(sut, {
    ...shared,
    SUT_RUNTIME_TARGET: JSON.stringify(LEGACY_SUT_RUNTIME_TARGET),
  })
  assert.equal(isolatedBrowserOrigin, browserOrigin)
  assert.equal(sutHealthEndpoints, healthEndpoints)
  assert.deepEqual(
    withSutRuntimeTargetEnv({ KEEP: '1' }, LEGACY_SUT_RUNTIME_TARGET),
    { KEEP: '1', ...sut }
  )
  assert.deepEqual(
    withRuntimeTargetEnv({ KEEP: '1' }, LEGACY_SUT_RUNTIME_TARGET),
    { KEEP: '1', ...shared }
  )
})

test('optional databaseUrl flows into INPUT_DB_URL without SUT decoration', () => {
  const target = {
    ...LEGACY_SUT_RUNTIME_TARGET,
    databaseUrl:
      'jdbc:mysql://127.0.0.1:3309/doughnut_e2e_wt_fixture?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true',
  }
  const env = runtimeTargetProcessEnv(target)
  assert.equal(env.INPUT_DB_URL, target.databaseUrl)
  assert.equal(env.SUT_RUNTIME_TARGET, undefined)
})
