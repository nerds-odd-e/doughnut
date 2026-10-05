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
import { runDevRestart } from './dev-restart.mjs'
import { runDevStart } from './dev-start.mjs'
import {
  allocateFreePort,
  closeServer,
  isPidAlive,
  isTcpListening,
  listenTcp,
} from './sut-isolated-fixtures.mjs'
import { makeStartSpy } from './sut-start-fixtures.mjs'

function startSpy() {
  const calls = []
  return {
    calls,
    runDevStartFn: async (options) => {
      calls.push(options)
      return 0
    },
  }
}

test('restart stops the running stack before starting', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const { pids } = await startStandInDevelopmentStack(t, checkout.root)
  const runtimeTarget = targetFor(checkout.root)
  const start = startSpy()
  let aliveWhenStarting

  await runDevRestart({
    checkoutRoot: checkout.root,
    runtimeTarget,
    runDevStartFn: (options) => {
      aliveWhenStarting = Object.values(pids).filter(isPidAlive)
      return start.runDevStartFn(options)
    },
  })

  assert.deepEqual(aliveWhenStarting, [])
  assert.deepEqual(start.calls, [
    { checkoutRoot: checkout.root, runtimeTarget },
  ])
})

test('restart with nothing running starts Development', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const start = startSpy()

  assert.equal(
    await runDevRestart({
      checkoutRoot: checkout.root,
      runtimeTarget: targetFor(checkout.root),
      runDevStartFn: start.runDevStartFn,
    }),
    0
  )

  assert.equal(start.calls.length, 1)
})

test('a port held by another process makes restart refuse to start and leaves the listener', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const { server, port } = await listenTcp()
  t.after(() => closeServer(server))
  const spawn = makeStartSpy()

  await assert.rejects(
    runDevRestart({
      checkoutRoot: checkout.root,
      runtimeTarget: targetFor(checkout.root, {
        backendPort: port,
        vitePort: await allocateFreePort(),
        lbListenPort: await allocateFreePort(),
      }),
      runDevStartFn: (options) =>
        runDevStart({ ...options, spawnFn: spawn.spawnFn }),
    }),
    new RegExp(`occupied \\(backend ${port}\\)`)
  )

  assert.equal(await isTcpListening(port), true)
  assert.equal(spawn.calls.length, 0)
})

test('linked worktree refuses Development restart', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const start = startSpy()

  await assert.rejects(
    runDevRestart({
      checkoutRoot: checkout.root,
      runtimeTarget: targetFor(checkout.root),
      runDevStartFn: start.runDevStartFn,
    }),
    /primary checkout.*linked worktree isolation/
  )
  assert.equal(start.calls.length, 0)
})
