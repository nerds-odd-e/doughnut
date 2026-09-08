import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { runSutHealthcheck } from './sut-healthcheck.mjs'
import {
  allocateFreePort,
  closeServer,
  completeIsolatedConfig,
  isTcpListening,
  listenHttpReady,
  listenTcp,
  startLiveOwner,
  writeIsolatedConfig,
  writeIsolatedE2ePorts,
} from './sut-isolated-fixtures.mjs'
import { startOwnedListeningOwner } from './sut-owned-listening-fixtures.mjs'

test('isolated health requires the live owner even when a foreign ready listener exists', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const ready = await listenHttpReady()
  t.after(() => closeServer(ready.server))
  writeIsolatedConfig(checkout.root, {
    ...completeIsolatedConfig,
    e2e: { ...completeIsolatedConfig.e2e, lbListenPort: ready.port },
  })
  const logs = []
  const health = await runSutHealthcheck({
    checkoutRoot: checkout.root,
    log: (line) => logs.push(line),
  })
  assert.equal(health.ok, false)
  assert.match(logs.join('\n'), /live SUT owner/)
  assert.equal(health.readinessResult.ok, false)
})

test('control-only live owner is unhealthy even when foreign endpoints are ready', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const backend = await listenTcp()
  const vite = await listenTcp()
  const ready = await listenHttpReady()
  t.after(() => closeServer(backend.server))
  t.after(() => closeServer(vite.server))
  t.after(() => closeServer(ready.server))
  writeIsolatedConfig(checkout.root, {
    ...completeIsolatedConfig,
    e2e: {
      ...completeIsolatedConfig.e2e,
      backendPort: backend.port,
      vitePort: vite.port,
      lbListenPort: ready.port,
    },
  })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  const health = await runSutHealthcheck({
    checkoutRoot: checkout.root,
    log: () => undefined,
  })
  assert.equal(health.ok, false)
  assert.equal(await isTcpListening(backend.port), true)
  assert.equal(await isTcpListening(vite.port), true)
  assert.equal(await isTcpListening(ready.port), true)
})

test('isolated health fails when a recorded endpoint listener is outside the application group', async (t) => {
  const foreignCases = [
    { omitService: 'backend', foreign: 'backend', listen: listenTcp },
    {
      omitService: 'frontend vite',
      foreign: 'frontend vite',
      listen: listenTcp,
    },
    { omitService: 'local LB', foreign: 'local LB', listen: listenHttpReady },
  ]
  for (const { omitService, foreign, listen } of foreignCases) {
    await t.test(`foreign ${foreign} listener`, async (t) => {
      const checkout = makePrimaryCheckout(t)
      const foreignListener = await listen()
      t.after(() => closeServer(foreignListener.server))

      const ports = {
        backendPort:
          foreign === 'backend'
            ? foreignListener.port
            : await allocateFreePort(),
        vitePort:
          foreign === 'frontend vite'
            ? foreignListener.port
            : await allocateFreePort(),
        lbListenPort:
          foreign === 'local LB'
            ? foreignListener.port
            : await allocateFreePort(),
      }

      writeIsolatedE2ePorts(checkout.root, ports)
      await startOwnedListeningOwner(t, checkout.root, {
        ...ports,
        omitService,
      })
      const health = await runSutHealthcheck({
        checkoutRoot: checkout.root,
        log: () => undefined,
      })
      assert.equal(health.ok, false)
      assert.equal(await isTcpListening(foreignListener.port), true)
    })
  }
})

test('isolated health passes when recorded listeners belong to the application group', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const ports = {
    backendPort: await allocateFreePort(),
    vitePort: await allocateFreePort(),
    lbListenPort: await allocateFreePort(),
  }
  writeIsolatedE2ePorts(checkout.root, ports)
  await startOwnedListeningOwner(t, checkout.root, ports)
  const health = await runSutHealthcheck({
    checkoutRoot: checkout.root,
    log: () => undefined,
  })
  assert.equal(health.ok, true)
  assert.deepEqual(
    health.tcpResults.map((result) => [result.service, result.port]),
    [
      ['backend', ports.backendPort],
      ['local LB', ports.lbListenPort],
      ['frontend vite', ports.vitePort],
    ]
  )
  assert.equal(
    health.readinessResult.url,
    `http://127.0.0.1:${ports.lbListenPort}/__lb__/ready`
  )
})
