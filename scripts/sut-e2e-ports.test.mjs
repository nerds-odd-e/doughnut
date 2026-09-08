import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { existsSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { setTimeout as delay } from 'node:timers/promises'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  closeServer,
  completeIsolatedConfig,
  identityAndPortsConfig,
  isTcpListening,
  listenTcp,
  readIsolatedConfig,
  runConfiguredStart,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  ensureIsolatedE2ePorts,
  readPublishedE2ePortClaims,
} from './sut-e2e-ports.mjs'
import {
  makeClaimRoot,
  portsModuleHref,
  spawnEnsurePorts,
  waitForClose,
} from './sut-e2e-port-test-helpers.mjs'
import { makeStartSpy } from './sut-start-fixtures.mjs'

async function waitForFile(filePath, timeoutMs = 3000) {
  const deadline = Date.now() + timeoutMs
  while (!existsSync(filePath)) {
    if (Date.now() >= deadline) {
      throw new Error(`timed out waiting for ${filePath}`)
    }
    await delay(20)
  }
}

test('explicit recorded ports are published as one claim', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const claimRoot = makeClaimRoot(t)
  writeIsolatedConfig(checkout.root, {
    ...identityAndPortsConfig,
    extra: 'keep-me',
  })

  const ports = await ensureIsolatedE2ePorts(checkout.root, { claimRoot })
  assert.deepEqual(ports, {
    backendPort: 19081,
    vitePort: 15174,
    lbListenPort: 15173,
  })
  const config = readIsolatedConfig(checkout.root)
  assert.equal(config.id, 'wt_a7c2')
  assert.equal(config.extra, 'keep-me')
  assert.deepEqual(config.e2e, identityAndPortsConfig.e2e)
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), {
    wt_a7c2: ports,
  })
  assert.equal(existsSync(path.join(claimRoot, 'lock')), false)
})

test('port claims serialize across cooperating checkout roots', async (t) => {
  const claimRoot = makeClaimRoot(t)
  const firstCheckout = makePrimaryCheckout(t)
  const secondCheckout = makePrimaryCheckout(t)
  writeIsolatedConfig(firstCheckout.root, identityAndPortsConfig)
  writeIsolatedConfig(secondCheckout.root, {
    id: 'wt_b8d3',
    e2e: {
      backendPort: 29081,
      vitePort: 25174,
      lbListenPort: 25173,
    },
  })
  const reached = path.join(claimRoot, 'hold-reached')
  const release = path.join(claimRoot, 'hold-release')

  const holder = spawn(
    process.execPath,
    [
      '--input-type=module',
      '-e',
      `import { writeFileSync, existsSync } from 'node:fs'
import { withE2ePortClaimLock } from ${JSON.stringify(portsModuleHref)}
await withE2ePortClaimLock(${JSON.stringify(claimRoot)}, () => {
  writeFileSync(${JSON.stringify(reached)}, 'held')
  while (!existsSync(${JSON.stringify(release)})) {
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 20)
  }
})
`,
    ],
    { encoding: 'utf8' }
  )
  const holderDone = waitForClose(holder)
  await waitForFile(reached)

  const waiter = spawnEnsurePorts(secondCheckout.root, claimRoot)
  const waiterDone = waitForClose(waiter)
  await delay(80)
  assert.equal(waiter.exitCode, null)
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), {})

  writeFileSync(release, 'go\n')
  const [holderResult, waiterResult] = await Promise.all([
    holderDone,
    waiterDone,
  ])
  assert.equal(holderResult.status, 0, holderResult.stderr)
  assert.equal(waiterResult.status, 0, waiterResult.stderr)

  await ensureIsolatedE2ePorts(firstCheckout.root, { claimRoot })
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), {
    wt_a7c2: {
      backendPort: 19081,
      vitePort: 15174,
      lbListenPort: 15173,
    },
    wt_b8d3: {
      backendPort: 29081,
      vitePort: 25174,
      lbListenPort: 25173,
    },
  })
})

test('published port claims do not own or terminate a foreign listener', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const claimRoot = makeClaimRoot(t)
  const { server, port } = await listenTcp()
  t.after(() => closeServer(server))
  writeIsolatedConfig(checkout.root, {
    ...completeIsolatedConfig,
    e2e: { ...completeIsolatedConfig.e2e, backendPort: port },
  })
  const spawnSpy = makeStartSpy()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawnSpy, {
      checkRealPorts: true,
      portClaimRoot: claimRoot,
    }),
    /already occupied/
  )
  assert.equal(spawnSpy.calls.length, 0)
  assert.equal(await isTcpListening(port), true)
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot).wt_a7c2, {
    backendPort: port,
    vitePort: 15174,
    lbListenPort: 15173,
  })
})
