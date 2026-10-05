import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
} from './backend-test-worktree-linked-fixtures.mjs'
import {
  startStandInDevelopmentStack,
  targetFor,
} from './dev-stack-fixtures.mjs'
import { runDevStart } from './dev-start.mjs'
import {
  allocateFreePort,
  closeServer,
  identityOnlyConfig,
  isPidAlive,
  isTcpListening,
  listenTcp,
} from './sut-isolated-fixtures.mjs'
import {
  healthyOnce,
  makeLogs,
  makeStartSpy,
  neverHealthy,
} from './sut-start-fixtures.mjs'

test('linked worktree refuses Development start without spawning', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const spawn = makeStartSpy()
  await assert.rejects(
    runDevStart({
      checkoutRoot: checkout.root,
      runtimeTarget: targetFor(checkout.root),
      spawnFn: spawn.spawnFn,
      isPortOccupiedFn: async () => false,
    }),
    /primary checkout.*linked worktree isolation/
  )
  assert.equal(spawn.calls.length, 0)
})

test('configured primary starts Development', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(identityOnlyConfig),
  })
  const runtimeTarget = targetFor(checkout.root)
  const spawn = makeStartSpy()
  const code = await runDevStart({
    checkoutRoot: checkout.root,
    runtimeTarget,
    spawnFn: spawn.spawnFn,
    isPortOccupiedFn: async () => false,
    healthcheckFn: healthyOnce,
  })

  assert.equal(code, 0)
  assert.equal(spawn.calls.length, 1)
})

test("this checkout's running stack refuses start, naming its services pid", async (t) => {
  const checkout = makePrimaryCheckout(t)
  const { pids } = await startStandInDevelopmentStack(t, checkout.root)
  const spawn = makeStartSpy()
  await assert.rejects(
    runDevStart({
      checkoutRoot: checkout.root,
      runtimeTarget: targetFor(checkout.root),
      spawnFn: spawn.spawnFn,
      isPortOccupiedFn: async () => false,
    }),
    new RegExp(`already running \\(services pid ${pids.services}\\)`)
  )
  assert.equal(spawn.calls.length, 0)
  assert.equal(isPidAlive(pids.services), true)
})

test('occupied Development port refuses without terminating the listener', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const { server, port } = await listenTcp()
  t.after(() => closeServer(server))
  const vitePort = await allocateFreePort()
  const lbListenPort = await allocateFreePort()
  const spawn = makeStartSpy()
  await assert.rejects(
    runDevStart({
      checkoutRoot: checkout.root,
      runtimeTarget: targetFor(checkout.root, {
        backendPort: port,
        vitePort,
        lbListenPort,
      }),
      spawnFn: spawn.spawnFn,
    }),
    /already occupied/
  )
  assert.equal(spawn.calls.length, 0)
  assert.equal(await isTcpListening(port), true)
})

test('free unconfigured primary starts Development, prints browser origin when healthy', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const runtimeTarget = targetFor(checkout.root, { lbListenPort: 5175 })
  const spawn = makeStartSpy()
  const logs = makeLogs()
  const code = await runDevStart({
    checkoutRoot: checkout.root,
    runtimeTarget,
    spawnFn: spawn.spawnFn,
    isPortOccupiedFn: async () => false,
    healthcheckFn: healthyOnce,
    timeoutMs: 5_000,
    pollMs: 50,
    log: logs.log,
    errLog: logs.errLog,
  })
  assert.equal(code, 0)
  assert.equal(spawn.calls.length, 1)
  assert.equal(spawn.calls[0][0], process.execPath)
  assert.match(spawn.calls[0][1][0], /development-services\.mjs$/)
  assert.equal(spawn.calls[0][2].cwd, checkout.root)
  assert.equal(spawn.calls[0][2].detached, true)
  assert.ok(logs.out.some((line) => /Development healthy/.test(line)))
  assert.ok(
    logs.out.some((line) => line === 'Browser origin: http://127.0.0.1:5175')
  )
})

test('Development start exits 1 when healthcheck never passes', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const runtimeTarget = targetFor(checkout.root)
  const spawn = makeStartSpy()
  const logs = makeLogs()
  const code = await runDevStart({
    checkoutRoot: checkout.root,
    runtimeTarget,
    spawnFn: spawn.spawnFn,
    isPortOccupiedFn: async () => false,
    healthcheckFn: neverHealthy,
    timeoutMs: 100,
    pollMs: 40,
    log: logs.log,
    errLog: logs.errLog,
  })
  assert.equal(code, 1)
  assert.equal(spawn.calls.length, 1)
  assert.ok(
    logs.err.some((line) =>
      /Development did not become healthy within the timeout/.test(line)
    )
  )
})
