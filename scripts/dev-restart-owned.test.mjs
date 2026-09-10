import assert from 'node:assert/strict'
import { writeFileSync } from 'node:fs'
import { test } from 'node:test'
import { runDevRestart } from './dev-restart.mjs'
import {
  allocateFreePort,
  listenersForPorts,
  makePrimaryCheckout,
  makeStartSpy,
  targetFor,
  trackingKill,
} from './dev-restart-fixtures.mjs'

test('idle free Development target starts without signalling', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const runtimeTarget = targetFor(checkout.root, {
    backendPort: await allocateFreePort(),
    vitePort: await allocateFreePort(),
    lbListenPort: await allocateFreePort(),
  })
  const kill = trackingKill()
  const start = makeStartSpy()
  /** @type {unknown[]} */
  const startOpts = []

  const code = await runDevRestart({
    checkoutRoot: checkout.root,
    runtimeTarget,
    getListenerPidsFn: async () => [],
    killFn: kill.killFn,
    runDevStartFn: async (opts) => {
      start.calls.push(['runDevStart'])
      startOpts.push(opts)
      return 0
    },
  })

  assert.equal(code, 0)
  assert.equal(kill.calls.length, 0)
  assert.equal(start.calls.length, 1)
  assert.equal(startOpts[0]?.runtimeTarget, runtimeTarget)
  assert.equal(startOpts[0]?.checkoutRoot, checkout.root)
})

test('owned Development group is stopped, waits for free ports, then starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const runtimeTarget = targetFor(checkout.root, {
    backendPort: await allocateFreePort(),
    vitePort: await allocateFreePort(),
    lbListenPort: await allocateFreePort(),
  })
  const groupId = 91001
  const listenerPids = [91002, 91003, 91004]
  writeFileSync(runtimeTarget.pidFile, String(groupId))
  const kill = trackingKill()
  const start = makeStartSpy()
  /** @type {unknown[]} */
  const startOpts = []
  let occupiedUntilSignalled = true

  const code = await runDevRestart({
    checkoutRoot: checkout.root,
    runtimeTarget,
    isProcessAliveFn: (pid) => {
      if (Math.abs(pid) !== groupId) return false
      return kill.calls.length === 0
    },
    getListenerPidsFn: listenersForPorts(
      new Map([
        [runtimeTarget.backendPort, [listenerPids[0]]],
        [runtimeTarget.vitePort, [listenerPids[1]]],
        [runtimeTarget.lbListenPort, [listenerPids[2]]],
      ])
    ),
    isOwnedByApplicationTreeFn: async () => true,
    killFn: kill.killFn,
    descendantPidsFn: async () => [...listenerPids],
    isPortOccupiedFn: async () => {
      if (kill.calls.length > 0) occupiedUntilSignalled = false
      return occupiedUntilSignalled
    },
    runDevStartFn: async (opts) => {
      start.calls.push(['runDevStart'])
      startOpts.push(opts)
      return 0
    },
  })

  assert.equal(code, 0)
  assert.ok(kill.calls.some((c) => c.signal === 'SIGTERM'))
  assert.ok(kill.calls.some((c) => c.pid === groupId || c.pid === -groupId))
  for (const listenerPid of listenerPids) {
    assert.ok(
      kill.calls.some((c) => c.pid === listenerPid && c.signal === 'SIGTERM')
    )
  }
  assert.equal(start.calls.length, 1)
  assert.equal(startOpts[0]?.runtimeTarget, runtimeTarget)
  assert.equal(startOpts[0]?.checkoutRoot, checkout.root)
  assert.equal(occupiedUntilSignalled, false)
})
