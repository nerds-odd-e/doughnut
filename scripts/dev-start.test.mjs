import assert from 'node:assert/strict'
import { writeFileSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { test } from 'node:test'
import {
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
} from './backend-test-worktree-linked-fixtures.mjs'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import { runDevStart } from './dev-start.mjs'
import {
  allocateFreePort,
  closeServer,
  identityOnlyConfig,
  isTcpListening,
  listenTcp,
} from './sut-isolated-fixtures.mjs'
import {
  healthyOnce,
  makeLogs,
  makeStartSpy,
  neverHealthy,
} from './sut-start-fixtures.mjs'

function targetFor(checkoutRoot, ports = {}) {
  return {
    ...DEVELOPMENT_RUNTIME_TARGET,
    ...ports,
    logFile: path.join(checkoutRoot, 'dev.log'),
    pidFile: path.join(checkoutRoot, 'dev.pid'),
  }
}

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
    /unconfigured primary|worktree isolation/
  )
  assert.equal(spawn.calls.length, 0)
})

test('configured primary refuses Development start without spawning', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(identityOnlyConfig),
  })
  const spawn = makeStartSpy()
  await assert.rejects(
    runDevStart({
      checkoutRoot: checkout.root,
      runtimeTarget: targetFor(checkout.root),
      spawnFn: spawn.spawnFn,
      isPortOccupiedFn: async () => false,
    }),
    /unconfigured primary|worktree isolation/
  )
  assert.equal(spawn.calls.length, 0)
})

test('live Development pid refuses duplicate start without signalling', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const runtimeTarget = targetFor(checkout.root)
  writeFileSync(runtimeTarget.pidFile, String(process.pid))
  const spawn = makeStartSpy()
  await assert.rejects(
    runDevStart({
      checkoutRoot: checkout.root,
      runtimeTarget,
      spawnFn: spawn.spawnFn,
      isPortOccupiedFn: async () => false,
    }),
    /already running/
  )
  assert.equal(spawn.calls.length, 0)
  try {
    process.kill(process.pid, 0)
  } catch {
    assert.fail('live Development pid must not be signalled')
  }
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

test('free unconfigured primary starts Development, writes pid, prints browser origin when healthy', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const runtimeTarget = targetFor(checkout.root, { lbListenPort: 5175 })
  const spawn = makeStartSpy(9090)
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
  assert.equal(await readFile(runtimeTarget.pidFile, 'utf8'), '9090')
  assert.ok(logs.out.some((line) => /Development healthy/.test(line)))
  assert.ok(
    logs.out.some((line) => line === 'Browser origin: http://127.0.0.1:5175')
  )
})

test('Development start exits 1 when healthcheck never passes', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const runtimeTarget = targetFor(checkout.root)
  const spawn = makeStartSpy(7070)
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
  assert.equal(await readFile(runtimeTarget.pidFile, 'utf8'), '7070')
  assert.ok(
    logs.err.some((line) =>
      /Development did not become healthy within the timeout/.test(line)
    )
  )
})
