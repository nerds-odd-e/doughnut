import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  closeServer,
  completeIsolatedConfig,
  isTcpListening,
  listenTcp,
  runConfiguredStart,
  startLiveOwner,
  withEnv,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  isolatedBrowserOrigin,
  isolatedRuntimeTargetFromConfig,
} from './sut-isolated-target.mjs'
import { verifyLiveSutOwner } from './sut-owner.mjs'
import { runSutServices, sutServiceArgs } from './sut-services.mjs'
import { makeMockChild, makeStartSpy } from './sut-start-fixtures.mjs'

test('configured isolated start omits mountebank and prints the recorded target', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const spawn = makeStartSpy()
  const logs = []
  const target = isolatedRuntimeTargetFromConfig(completeIsolatedConfig)

  const code = await runConfiguredStart(checkout.root, spawn, {
    log: (line) => logs.push(line),
  })
  assert.equal(code, 0)
  assert.equal(spawn.calls.length, 1)
  const env = spawn.calls[0][2].env
  assert.equal(env.INPUT_DB_URL, target.databaseUrl)
  assert.equal(env.SERVER_PORT, '19081')
  assert.equal(env.SUT_OWNER_TOKEN.length > 0, true)
  assert.match(env.SUT_OWNER_CONTROL_PATH, /owner\.sock$/)
  assert.equal(JSON.parse(env.SUT_RUNTIME_TARGET).mountebankPort, undefined)
  assert.ok(
    logs.some((line) => line === 'Selected database: doughnut_e2e_wt_a7c2')
  )
  assert.ok(
    logs.some(
      (line) => line === `Browser origin: ${isolatedBrowserOrigin(target)}`
    )
  )

  const serviceSpawn = []
  runSutServices({
    runtimeTarget: target,
    spawnFn: (cmd, args, opts) => {
      serviceSpawn.push({ cmd, args, opts })
      return makeMockChild()
    },
    logWriter: { write: () => undefined, close: () => undefined },
  })
  assert.deepEqual(serviceSpawn[0].args, sutServiceArgs(target))
  assert.equal(serviceSpawn[0].args.includes('start:mb'), false)
})

test('duplicate isolated start refuses while the live owner remains', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  const spawn = makeStartSpy()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawn),
    /already running/
  )
  assert.equal(spawn.calls.length, 0)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, true)
})

test('occupied isolated ports refuse without terminating the foreign listener', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const { server, port } = await listenTcp()
  t.after(() => closeServer(server))
  writeIsolatedConfig(checkout.root, {
    ...completeIsolatedConfig,
    e2e: { ...completeIsolatedConfig.e2e, backendPort: port },
  })
  const spawn = makeStartSpy()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawn, { checkRealPorts: true }),
    /already occupied/
  )
  assert.equal(spawn.calls.length, 0)
  assert.equal(await isTcpListening(port), true)
})

test('missing recorded E2E database refuses before spawn', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const spawn = makeStartSpy()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawn, {
      databaseExistsFn: () => false,
    }),
    /does not exist|will not create/
  )
  assert.equal(spawn.calls.length, 0)
})

test('conflicting datasource and port overrides refuse before spawn', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  withEnv(t, {
    INPUT_DB_URL:
      'jdbc:mysql://127.0.0.1:3309/doughnut_e2e_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true',
  })
  const spawn = makeStartSpy()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawn),
    /Conflicting INPUT_DB_URL/
  )
  assert.equal(spawn.calls.length, 0)
})

test('matching overrides remain usable for the assigned isolated target', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const target = isolatedRuntimeTargetFromConfig(completeIsolatedConfig)
  withEnv(t, {
    INPUT_DB_URL: target.databaseUrl,
    SERVER_PORT: '19081',
  })
  const spawn = makeStartSpy()
  const code = await runConfiguredStart(checkout.root, spawn)
  assert.equal(code, 0)
  assert.equal(spawn.calls.length, 1)
  assert.equal(spawn.calls[0][2].env.INPUT_DB_URL, target.databaseUrl)
})
