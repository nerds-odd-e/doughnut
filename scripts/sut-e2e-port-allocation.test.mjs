import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  closeServer,
  identityAndPortsConfig,
  identityOnlyConfig,
  isTcpListening,
  listenTcp,
  readIsolatedConfig,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  ensureIsolatedE2ePorts,
  readPublishedE2ePortClaims,
} from './sut-e2e-ports.mjs'
import {
  assertAllocatedIsolatedPorts,
  makeClaimRoot,
  spawnEnsurePorts,
  waitForClose,
} from './sut-e2e-port-test-helpers.mjs'

test('identity-only allocation records three distinct non-reserved ports', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const claimRoot = makeClaimRoot(t)
  writeIsolatedConfig(checkout.root, {
    ...identityOnlyConfig,
    extra: 'keep-me',
  })

  const ports = await ensureIsolatedE2ePorts(checkout.root, { claimRoot })
  assertAllocatedIsolatedPorts(ports)
  const config = readIsolatedConfig(checkout.root)
  assert.equal(config.id, 'wt_a7c2')
  assert.equal(config.extra, 'keep-me')
  assert.deepEqual(config.e2e, ports)
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), {
    wt_a7c2: ports,
  })
  const reused = await ensureIsolatedE2ePorts(checkout.root, { claimRoot })
  assert.deepEqual(reused, ports)
})

test('partial recorded ports refuse instead of allocating', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const claimRoot = makeClaimRoot(t)
  writeIsolatedConfig(checkout.root, {
    id: 'wt_a7c2',
    e2e: { backendPort: 19081 },
  })
  await assert.rejects(
    ensureIsolatedE2ePorts(checkout.root, { claimRoot }),
    /Missing or invalid: e2e.vitePort, e2e.lbListenPort/
  )
  assert.deepEqual(readIsolatedConfig(checkout.root).e2e, {
    backendPort: 19081,
  })
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), {})
})

test('auto-allocation skips claimed ports and occupied TCP listeners', async (t) => {
  const claimed = makePrimaryCheckout(t)
  const fresh = makePrimaryCheckout(t)
  const claimRoot = makeClaimRoot(t)
  const { server, port } = await listenTcp()
  t.after(() => closeServer(server))
  writeIsolatedConfig(claimed.root, identityAndPortsConfig)
  writeIsolatedConfig(fresh.root, { id: 'wt_b8d3' })

  await ensureIsolatedE2ePorts(claimed.root, { claimRoot })
  const ports = await ensureIsolatedE2ePorts(fresh.root, { claimRoot })
  assertAllocatedIsolatedPorts(ports)
  const claimedValues = [19081, 15174, 15173, port]
  for (const value of Object.values(ports)) {
    assert.equal(claimedValues.includes(value), false)
  }
  assert.equal(await isTcpListening(port), true)
})

test('concurrent identity-only checkouts publish distinct claims', async (t) => {
  const claimRoot = makeClaimRoot(t)
  const first = makePrimaryCheckout(t)
  const second = makePrimaryCheckout(t)
  writeIsolatedConfig(first.root, { id: 'wt_peer_a' })
  writeIsolatedConfig(second.root, { id: 'wt_peer_b' })

  const [firstResult, secondResult] = await Promise.all([
    waitForClose(spawnEnsurePorts(first.root, claimRoot)),
    waitForClose(spawnEnsurePorts(second.root, claimRoot)),
  ])
  assert.equal(firstResult.status, 0, firstResult.stderr)
  assert.equal(secondResult.status, 0, secondResult.stderr)

  const firstPorts = readIsolatedConfig(first.root).e2e
  const secondPorts = readIsolatedConfig(second.root).e2e
  assertAllocatedIsolatedPorts(firstPorts)
  assertAllocatedIsolatedPorts(secondPorts)
  assert.notDeepEqual(firstPorts, secondPorts)
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), {
    wt_peer_a: firstPorts,
    wt_peer_b: secondPorts,
  })
})

test('competing same-checkout identity-only starts publish one claim', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const claimRoot = makeClaimRoot(t)
  writeIsolatedConfig(checkout.root, identityOnlyConfig)

  const [firstResult, secondResult] = await Promise.all([
    waitForClose(spawnEnsurePorts(checkout.root, claimRoot)),
    waitForClose(spawnEnsurePorts(checkout.root, claimRoot)),
  ])
  assert.equal(firstResult.status, 0, firstResult.stderr)
  assert.equal(secondResult.status, 0, secondResult.stderr)
  const ports = readIsolatedConfig(checkout.root).e2e
  assertAllocatedIsolatedPorts(ports)
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), {
    wt_a7c2: ports,
  })
})
