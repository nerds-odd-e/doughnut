import assert from 'node:assert/strict'
import { writeFileSync } from 'node:fs'
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
import { makeStartSpy } from './sut-start-fixtures.mjs'

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

test('free unconfigured primary reaches the Development spawn seam', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const spawn = makeStartSpy()
  const logs = []
  const code = await runDevStart({
    checkoutRoot: checkout.root,
    runtimeTarget: targetFor(checkout.root),
    spawnFn: spawn.spawnFn,
    isPortOccupiedFn: async () => false,
    log: (line) => logs.push(line),
  })
  assert.equal(code, 0)
  assert.equal(spawn.calls.length, 1)
  assert.equal(spawn.calls[0][0], process.execPath)
  assert.match(spawn.calls[0][1][0], /development-services\.mjs$/)
  assert.equal(spawn.calls[0][2].cwd, checkout.root)
  assert.equal(spawn.calls[0][2].detached, true)
  assert.ok(logs.some((line) => /spawn seam reached/.test(line)))
})
