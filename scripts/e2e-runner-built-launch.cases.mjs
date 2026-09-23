import { BUILT_RUNTIME_TARGET } from './e2e-runner-lifetime-fixtures.mjs'
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { allocateFreePort } from './sut-isolated-fixtures.mjs'
import { sutServiceArgs } from './sut-services.mjs'
import {
  healthEndpoints,
  listOccupiedApplicationPorts,
  runtimeTargetProcessEnv,
} from './local-runtime-target.mjs'

test('built target launch data: sutServiceArgs omits frontend:sut and uses local:lb (no Vite)', () => {
  const args = sutServiceArgs(BUILT_RUNTIME_TARGET)
  assert.equal(
    args.includes('frontend:sut'),
    false,
    'must not start Vite dev server'
  )
  assert.equal(args.includes('local:lb'), true, 'must start the local LB')
  assert.equal(
    args.includes('local:lb:vite'),
    false,
    'must use local:lb not local:lb:vite'
  )
  assert.equal(
    args.includes('backend:sut:ci'),
    true,
    'must start bootRun without Gradle watch'
  )
  assert.equal(
    args.includes('backend:sut'),
    false,
    'must not start backend:watch alongside bootRun'
  )
  assert.equal(
    args.includes('start:mb'),
    true,
    'must start Mountebank (canonical port)'
  )
})

test('built target launch data: runtimeTargetProcessEnv omits Vite upstream + dev port', () => {
  const env = runtimeTargetProcessEnv(BUILT_RUNTIME_TARGET)
  assert.equal(
    env.LOCAL_LB_VITE_UPSTREAM,
    undefined,
    'no Vite upstream for built target'
  )
  assert.equal(
    env.FRONTEND_DEV_PORT,
    undefined,
    'no Vite dev port for built target'
  )
  assert.equal(env.SERVER_PORT, '9081', 'backend port preserved')
  assert.equal(
    env.LOCAL_LB_BACKEND,
    'http://127.0.0.1:9081',
    'LB backend preserved'
  )
  assert.equal(env.LOCAL_LB_LISTEN_PORT, '5173', 'LB listen port preserved')
  assert.equal(
    env.FRONTEND_BACKEND_ORIGIN,
    'http://127.0.0.1:9081',
    'backend origin preserved'
  )
})

test('built target launch data: healthEndpoints omits the frontend vite TCP check', () => {
  const endpoints = healthEndpoints(BUILT_RUNTIME_TARGET)
  const services = endpoints.tcpChecks.map((c) => c.service)
  assert.equal(
    services.includes('frontend vite'),
    false,
    'no Vite readiness check for built target'
  )
  assert.equal(services.includes('backend'), true, 'backend check preserved')
  assert.equal(services.includes('local LB'), true, 'LB check preserved')
  assert.equal(
    services.includes('mountebank'),
    true,
    'Mountebank check preserved'
  )
  assert.equal(
    endpoints.readinessUrl,
    'http://127.0.0.1:5173/__lb__/ready',
    'readiness URL preserved (LB + backend, no Vite)'
  )
})

test('built target launch data: foreign-listener refusal skips the Vite port', async () => {
  // A foreign listener on the Vite port must NOT trigger refusal for a built
  // target (the Vite port is not used). A foreign listener on the LB port
  // still must.
  const freeVite = await allocateFreePort()
  const freeLb = await allocateFreePort()
  const builtTarget = {
    backendPort: 9081,
    vitePort: freeVite,
    lbListenPort: freeLb,
    mountebankPort: 2525,
    built: true,
  }
  // Vite port occupied → not reported (built target skips it).
  const occupiedNoVite = await listOccupiedApplicationPorts(
    builtTarget,
    async (port) => port === freeVite
  )
  assert.deepEqual(
    occupiedNoVite,
    [],
    'built target must not report a foreign Vite listener'
  )
  // LB port occupied → reported (built target still checks backend + LB).
  const occupiedLb = await listOccupiedApplicationPorts(
    builtTarget,
    async (port) => port === freeLb
  )
  assert.deepEqual(
    occupiedLb,
    [`local LB ${freeLb}`],
    'built target must report a foreign LB listener'
  )
})
