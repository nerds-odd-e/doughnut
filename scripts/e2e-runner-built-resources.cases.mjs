import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'

import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  allocateFreePort,
  closeServer,
  isPidAlive,
  listenTcp,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
} from './sut-isolated-fixtures.mjs'
import { SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC } from './isolated-cypress-spec-selection.mjs'
import { spawnIdlePrivateMockHandle } from './isolated-openai-mock-test-fixtures.mjs'
import { sutOwnerLockDir } from './sut-owner.mjs'
import { healthyOnce } from './sut-start-fixtures.mjs'
import { E2E_RUNNER_OWNS_LIFETIME_ENV_KEY } from './isolated-cypress.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import { LEGACY_SUT_RUNTIME_TARGET } from './sut-runtime-target.mjs'
import {
  makeCypressChild,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  healthcheckWaitingForPids,
  isPortStillListening,
  trackOwnedTree,
  primaryLifetimeOpts,
  BUILT_RUNTIME_TARGET,
} from './e2e-runner-lifetime-fixtures.mjs'

test('built-asset batch: mock-requiring spec does NOT start a wrapper-owned mock (primary path, approved null)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let mockStarts = 0
  let capturedEnv = null

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...primaryLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    runtimeTarget: BUILT_RUNTIME_TARGET,
    startPrivateOpenAiMockFn: async () => {
      mockStarts += 1
      return spawnIdlePrivateMockHandle()
    },
    spawnCypress: (opts) => {
      capturedEnv = opts.env
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
      })
    },
  })

  assert.equal(code, 0)
  assert.equal(
    mockStarts,
    0,
    'built (primary) path must not start a wrapper-owned mock even for the mock-requiring spec'
  )
  assert.equal(
    capturedEnv && capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
    undefined,
    'Cypress env must not signal wrapper-owned lifetime'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

test('built-asset batch: foreign listener on the Vite port does NOT refuse (built target skips Vite)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  // A foreign listener on the Vite port: the built target does not use Vite,
  // so this must NOT trigger refusal. Use ephemeral ports to avoid touching
  // real canonical ports.
  const foreignVite = await listenTcp()
  t.after(() => closeServer(foreignVite.server))
  const freeBackend = await allocateFreePort()
  const freeLb = await allocateFreePort()
  const runtimeTarget = {
    backendPort: freeBackend,
    vitePort: foreignVite.port,
    lbListenPort: freeLb,
    mountebankPort: 2525,
    built: true,
  }
  let startCalled = false

  const code = await runE2eBatch({
    argv: cypressArgv(),
    checkoutRoot: checkout.root,
    runtimeTarget,
    // Real port check: the foreign Vite listener is detected on its port.
    isPortOccupiedFn: async (port) => port === foreignVite.port,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start — but only if a USED port is foreign')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  // The built target skips the Vite port in foreign-listener refusal, so the
  // foreign Vite listener does NOT block the start. startLifetime IS called
  // (it throws here, returning 1, but proving refusal did not happen).
  assert.equal(code, 1)
  assert.equal(
    startCalled,
    true,
    'must NOT refuse on a foreign Vite listener (built target)'
  )
  // The foreign Vite listener survives — not signalled.
  assert.equal(
    await isPortStillListening(foreignVite.port),
    true,
    'foreign Vite listener must survive'
  )
})

test('CI built overlay: SUT_RUNTIME_TARGET {"built":true} probes legacy application ports', async (t) => {
  const previous = process.env.SUT_RUNTIME_TARGET
  process.env.SUT_RUNTIME_TARGET = '{"built":true}'
  t.after(() => {
    if (previous === undefined) delete process.env.SUT_RUNTIME_TARGET
    else process.env.SUT_RUNTIME_TARGET = previous
  })

  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const probedPorts = []
  let capturedTarget = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: async (hcOpts) => {
        capturedTarget = hcOpts.runtimeTarget
        return healthyOnce()
      },
    }),
    isPortOccupiedFn: async (port) => {
      probedPorts.push(port)
      return false
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 0)
  assert.deepEqual(probedPorts, [
    LEGACY_SUT_RUNTIME_TARGET.backendPort,
    LEGACY_SUT_RUNTIME_TARGET.lbListenPort,
  ])
  assert.deepEqual(capturedTarget, {
    ...LEGACY_SUT_RUNTIME_TARGET,
    built: true,
  })
})
