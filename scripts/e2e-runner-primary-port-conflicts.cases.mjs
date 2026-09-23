import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  allocateFreePort,
  closeServer,
  listenTcp,
} from './sut-isolated-fixtures.mjs'
import { sutOwnerLockDir } from './sut-owner.mjs'
import { allocatePrimaryOpenAiMockPorts, runE2eBatch } from './e2e-runner.mjs'
import {
  makeCypressChild,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  listenTcpOnPort,
  isPortStillListening,
} from './e2e-runner-lifetime-fixtures.mjs'

test('primary batch: foreign listener on a canonical application port refuses without adoption or signalling', async (t) => {
  const checkout = makePrimaryCheckout(t)
  // Use a custom primary runtimeTarget with ephemeral ports so the real port
  // check can detect a real foreign listener without touching live canonical
  // ports (5173/5174/9081) that may be in use in the developer environment.
  const foreignListener = await listenTcp()
  t.after(() => closeServer(foreignListener.server))
  const freeBackend = await allocateFreePort()
  const freeVite = await allocateFreePort()
  const runtimeTarget = {
    backendPort: freeBackend,
    vitePort: freeVite,
    lbListenPort: foreignListener.port,
    mountebankPort: 2525,
  }
  let startCalled = false

  const code = await runE2eBatch({
    argv: cypressArgv(),
    checkoutRoot: checkout.root,
    runtimeTarget,
    // Real port check: the foreign listener on the lb port is detected.
    isPortOccupiedFn: async (port) => port === foreignListener.port,
    startLifetime: async () => {
      startCalled = true
      throw new Error(
        'must not start when a canonical port is foreign-occupied'
      )
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1, 'must refuse when a canonical port is foreign-occupied')
  assert.equal(startCalled, false, 'must refuse before spawning the supervisor')
  // Zero foreign signals: the foreign listener is still alive.
  assert.equal(
    await isPortStillListening(foreignListener.port),
    true,
    'foreign listener must not be signalled'
  )
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

test('primary batch: mixed owned/foreign conflict refuses without signalling any foreign listener', async (t) => {
  const checkout = makePrimaryCheckout(t)
  // Two foreign listeners on two of the three canonical application ports;
  // the third is free. The run must still refuse (mixed conflict) without
  // signalling either foreign listener.
  const foreignLb = await listenTcp()
  const foreignVite = await listenTcp()
  t.after(() => closeServer(foreignLb.server))
  t.after(() => closeServer(foreignVite.server))
  const freeBackend = await allocateFreePort()
  const runtimeTarget = {
    backendPort: freeBackend,
    vitePort: foreignVite.port,
    lbListenPort: foreignLb.port,
    mountebankPort: 2525,
  }
  let startCalled = false

  const code = await runE2eBatch({
    argv: cypressArgv(),
    checkoutRoot: checkout.root,
    runtimeTarget,
    isPortOccupiedFn: async (port) =>
      port === foreignLb.port || port === foreignVite.port,
    startLifetime: async () => {
      startCalled = true
      throw new Error('must not start on a mixed owned/foreign conflict')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1, 'must refuse on a mixed owned/foreign conflict')
  assert.equal(startCalled, false, 'must refuse before spawning')
  // Zero foreign signals: both foreign listeners survive.
  for (const { port } of [foreignLb, foreignVite]) {
    assert.equal(
      await isPortStillListening(port),
      true,
      `foreign listener on ${port} must survive`
    )
  }
})

test('primary mock: occupied canonical Mountebank port refuses without adoption (allocator unit)', async (t) => {
  // Foreign listener on the canonical Mountebank management port (2525).
  // Skip if 2525 is already occupied in the live environment (e.g. a
  // developer's Mountebank) — the refusal behavior is also covered by the
  // application-port foreign-listener tests above.
  const mbPort = 2525
  let foreignMb
  try {
    foreignMb = await listenTcpOnPort(mbPort)
  } catch {
    t.skip('port 2525 already occupied in this environment; skipping')
    return
  }
  t.after(() => closeServer(foreignMb.server))

  // The primary allocator must refuse before any mock child is spawned; a
  // foreign listener on the canonical management port is refusal, never
  // adoption. The serving port (5001) uses the same ownership check, so this
  // covers the canonical mock-port refusal behavior.
  await assert.rejects(
    allocatePrimaryOpenAiMockPorts(),
    /refuses foreign management listener on port 2525/,
    'must refuse a foreign listener on the canonical Mountebank port'
  )
  // The foreign listener is not signalled by the allocator.
  assert.equal(
    await isPortStillListening(mbPort),
    true,
    'foreign Mountebank must not be signalled'
  )
})
