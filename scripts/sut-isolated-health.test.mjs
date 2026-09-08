import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
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
import {
  descendantPidsByParentWalk,
  getListenerPids,
  processGroupId,
} from './sut-listener-pids.mjs'
import { claimSutOwnership, startSutOwnerControl } from './sut-owner.mjs'
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

test('isolated health passes when a listener is outside PGID but under the application tree', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const ports = {
    backendPort: await allocateFreePort(),
    vitePort: await allocateFreePort(),
    lbListenPort: await allocateFreePort(),
  }
  writeIsolatedE2ePorts(checkout.root, ports)

  const owner = await claimSutOwnership(checkout.root)
  const listenerSource = `
import net from 'node:net'
net.createServer((s) => s.end()).listen(${ports.backendPort}, '127.0.0.1')
setInterval(() => {}, 1000)
`
  const midSource = `
import { spawn } from 'node:child_process'
const listener = spawn(process.execPath, ['-e', ${JSON.stringify(listenerSource)}], { detached: true, stdio: 'ignore' })
listener.unref()
setInterval(() => {}, 1000)
`
  const child = spawn(
    process.execPath,
    [
      '-e',
      `import { spawn } from 'node:child_process'
import http from 'node:http'
import net from 'node:net'
net.createServer((s) => s.end()).listen(${ports.vitePort}, '127.0.0.1')
http.createServer((_q, r) => { r.statusCode = 200; r.end('ready') }).listen(${ports.lbListenPort}, '127.0.0.1')
spawn(process.execPath, ['-e', ${JSON.stringify(midSource)}], { stdio: 'ignore' })
setInterval(() => {}, 1000)
`,
    ],
    { stdio: 'ignore', detached: true, env: process.env }
  )
  child.unref()
  t.after(async () => {
    for (const pid of await descendantPidsByParentWalk(child.pid)) {
      try {
        process.kill(pid, 'SIGKILL')
      } catch {
        // already gone
      }
    }
    try {
      process.kill(-child.pid, 'SIGKILL')
    } catch {
      // already gone
    }
    try {
      child.kill('SIGKILL')
    } catch {
      // already gone
    }
  })
  const server = await startSutOwnerControl({
    ...owner,
    getApplicationGroupId: () => child.pid,
  })
  t.after(() => server.close())

  const deadline = Date.now() + 5_000
  while (Date.now() < deadline) {
    if (
      (await isTcpListening(ports.backendPort)) &&
      (await isTcpListening(ports.vitePort)) &&
      (await isTcpListening(ports.lbListenPort))
    ) {
      break
    }
    await new Promise((resolve) => setTimeout(resolve, 20))
  }
  assert.equal(await isTcpListening(ports.backendPort), true)

  const backendPids = await getListenerPids(ports.backendPort)
  assert.equal(backendPids.length, 1)
  const backendPgid = await processGroupId(backendPids[0])
  assert.notEqual(
    backendPgid,
    child.pid,
    'backend listener must sit outside the application PGID'
  )

  const health = await runSutHealthcheck({
    checkoutRoot: checkout.root,
    log: () => undefined,
  })
  assert.equal(health.ok, true)
})
