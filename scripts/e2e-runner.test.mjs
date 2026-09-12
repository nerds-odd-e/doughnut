import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { existsSync, readFileSync } from 'node:fs'
import net from 'node:net'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  allocateFreePort,
  closeServer,
  isPidAlive,
  listenTcp,
  spawnForeignProcess,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
  SUPPORTED_ISOLATED_MCP_SPEC,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
  SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC,
} from './isolated-cypress-spec-selection.mjs'
import { spawnIdlePrivateMockHandle } from './isolated-openai-mock-test-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import { healthyOnce, neverHealthy } from './sut-start-fixtures.mjs'
import {
  E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_MOCK_PGID_ENV_KEY,
  E2E_RUNNER_OWNS_LIFETIME_ENV_KEY,
  E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY,
} from './isolated-cypress.mjs'
import {
  allocatePrimaryOpenAiMockPorts,
  defaultSpawnCypress,
  defaultSpawnCypressOpen,
  runE2eBatch,
  runE2eInteractive,
} from './e2e-runner.mjs'
import { sutServiceArgs } from './sut-services.mjs'
import {
  healthEndpoints,
  listOccupiedApplicationPorts,
  runtimeTargetProcessEnv,
} from './local-runtime-target.mjs'
import { LEGACY_SUT_RUNTIME_TARGET } from './sut-runtime-target.mjs'

function makeCypressChild(exitCode, onSpawn) {
  const child = new EventEmitter()
  child.pid = 0
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.kill = () => undefined
  child.unref = () => undefined
  // emit exit on next tick so listeners attach first; onSpawn runs first so
  // tests can observe the live owned tree before shutdown.
  queueMicrotask(async () => {
    try {
      await onSpawn?.()
    } catch {
      // observation best-effort; do not block the exit
    }
    child.emit('exit', exitCode, null)
  })
  return child
}

/**
 * A Cypress child that stays alive until `.kill()` is called, then emits exit
 * with the signal. Used to exercise cancellation during the Cypress run.
 */
function makeLiveCypressChild(onSpawn) {
  const child = new EventEmitter()
  child.pid = 0
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.killed = false
  child.kill = (signal) => {
    if (child.killed) return
    child.killed = signal || 'SIGTERM'
    queueMicrotask(() => child.emit('exit', null, child.killed))
  }
  child.unref = () => undefined
  if (onSpawn) {
    queueMicrotask(async () => {
      try {
        await onSpawn()
      } catch {
        // observation best-effort
      }
    })
  }
  return child
}

/**
 * A cancellation handle driven manually by `trigger()` instead of process
 * signals, so boundary tests control timing without relying on OS signals.
 */
function manualCancel() {
  const controller = new AbortController()
  return {
    signal: controller.signal,
    isTriggered: () => controller.signal.aborted,
    onTriggered: (cb) => {
      if (controller.signal.aborted) {
        cb()
        return () => undefined
      }
      controller.signal.addEventListener('abort', cb, { once: true })
      return () => controller.signal.removeEventListener('abort', cb)
    },
    detach: () => undefined,
    trigger: () => controller.abort(),
  }
}

function makeCypressLaunchError(error, onSpawn) {
  return () => {
    const child = new EventEmitter()
    child.pid = 0
    child.kill = () => undefined
    child.unref = () => undefined
    queueMicrotask(async () => {
      try {
        await onSpawn?.()
      } catch {
        // observation best-effort
      }
      child.emit('error', error)
    })
    return child
  }
}

function isolatedLifetimeOpts(checkoutRoot, standIn, extra = {}) {
  return {
    checkoutRoot,
    spawnFn: standIn.spawnFn,
    logFile: path.join(checkoutRoot, 'sut.log'),
    pidFile: path.join(checkoutRoot, 'sut.pid'),
    timeoutMs: extra.timeoutMs ?? 5_000,
    pollMs: extra.pollMs ?? 50,
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: extra.healthcheckFn ?? healthyOnce,
    databaseExistsFn: () => true,
    portClaimRoot: path.join(checkoutRoot, '.doughnut-e2e-port-claims'),
    isPortOccupiedFn: async () => false,
  }
}

/**
 * Healthcheck that resolves only after the standIn has written its owned-pids
 * file. The standIn awaits `startSutOwnerControlFromEnv()` before spawning the
 * grandchild, so pids-present implies the owner control server is listening —
 * which the wrapper needs to acquire the runner lease for a mock batch.
 */
function healthcheckWaitingForPids(pidsFile) {
  return async () => {
    try {
      const pids = JSON.parse(readFileSync(pidsFile, 'utf8'))
      if (Number.isInteger(pids.leader) && pids.leader > 0) {
        return {
          ok: true,
          tcpResults: [],
          readinessResult: { ok: true },
          exitCode: 0,
        }
      }
    } catch {
      // pids file not written yet
    }
    return {
      ok: false,
      tcpResults: [],
      readinessResult: { ok: false },
      exitCode: 1,
    }
  }
}

function cypressArgv(spec = SUPPORTED_ISOLATED_CYPRESS_SPEC) {
  return ['--spec', spec]
}

/**
 * Listen on a specific TCP port (for foreign-listener refusal tests on
 * canonical ports). Returns `{ server, port }`. Rejects if the port is
 * already occupied.
 */
function listenTcpOnPort(port) {
  return new Promise((resolve, reject) => {
    const server = net.createServer((socket) => socket.end())
    server.once('error', reject)
    server.listen(port, '127.0.0.1', () => {
      resolve({ server, port })
    })
  })
}

function isPortStillListening(port) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host: '127.0.0.1', port }, () => {
      socket.end()
      resolve(true)
    })
    socket.on('error', () => resolve(false))
  })
}

function trackOwnedTree(t, getOwned) {
  t.after(() => {
    const owned = getOwned()
    if (Number.isInteger(owned?.leader) && owned.leader > 0) {
      try {
        process.kill(-owned.leader, 'SIGKILL')
      } catch {
        // already gone
      }
    }
    for (const pid of [owned?.leader, owned?.grandchild]) {
      if (!Number.isInteger(pid) || pid <= 0) continue
      try {
        process.kill(pid, 'SIGKILL')
      } catch {
        // already gone
      }
    }
  })
}

test('supported multi-feature batch shares one owned stack', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(
      `${SUPPORTED_ISOLATED_CYPRESS_SPEC},${SUPPORTED_ISOLATED_MCP_SPEC}`
    ),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: ({ specs }) => {
      assert.deepEqual(specs, [
        SUPPORTED_ISOLATED_CYPRESS_SPEC,
        SUPPORTED_ISOLATED_MCP_SPEC,
      ])
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
      })
    },
  })

  assert.equal(code, 0)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('failed startup: readiness failure cleans partial owned work and returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressSpawned = false

  // Capture owned pids while the tree is alive (before the readiness timeout
  // settles and shutdown reaps it).
  const pidsPromise = waitForOwnedPids(standIn.pidsFile)
  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: neverHealthy,
      timeoutMs: 1_500,
    }),
    spawnCypress: () => {
      cypressSpawned = true
      return makeCypressChild(0)
    },
  })
  state.owned = await pidsPromise

  assert.equal(code, 1)
  assert.equal(
    cypressSpawned,
    false,
    'Cypress must not run after readiness failure'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('failed Cypress launch: cleans owned work and returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: makeCypressLaunchError(
      Object.assign(new Error('spawn EACCES'), { code: 'EACCES' }),
      async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }
    ),
  })

  assert.equal(code, 1)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('nonzero test result: cleans owned work and returns the nonzero result', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(7, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }),
  })

  assert.equal(code, 7, 'must preserve Cypress nonzero exit code')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('peer survival: a foreign process is not signalled by the batch', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const foreign = spawnForeignProcess()
  t.after(() => {
    try {
      foreign.kill('SIGKILL')
    } catch {
      // already gone
    }
  })

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }),
  })

  assert.equal(code, 0)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(isPidAlive(foreign.pid), true, 'foreign peer must survive')
})

test('unsupported spec selection refuses before the stack starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  let startCalled = false

  const code = await runE2eBatch({
    argv: ['--spec', 'e2e_test/features/book_reading/epub_book.feature'],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1)
  assert.equal(startCalled, false, 'must refuse before starting the stack')
})

test('private-mock batch: owned mock stays alive across the batch and stops with the invocation', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let mockStarts = 0
  let mockStopCalls = 0
  let mockPid = 0

  const code = await runE2eBatch({
    argv: cypressArgv(
      `${SUPPORTED_ISOLATED_CYPRESS_SPEC},${SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC}`
    ),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      mockStarts += 1
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      // Wrap stop to count calls — the mock must be stopped exactly once
      // (at invocation shutdown), never between specs.
      const realStop = handle.stop
      handle.stop = async () => {
        mockStopCalls += 1
        await realStop()
      }
      return handle
    },
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        // The mock is alive during the Cypress run (no premature release).
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(mockStopCalls, 0, 'mock must not be stopped mid-batch')
      }),
  })

  assert.equal(code, 0)
  assert.equal(mockStarts, 1, 'mock must start exactly once for the batch')
  assert.equal(mockStopCalls, 1, 'mock must be stopped once at invocation end')
  assert.equal(isPidAlive(mockPid), false)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('private Wikidata-mock batch: owned mock stays alive across the batch and stops with the invocation', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let mockStarts = 0
  let mockStopCalls = 0
  let mockPid = 0
  let capturedEnv = null

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateWikidataMockFn: async () => {
      mockStarts += 1
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      const realStop = handle.stop
      handle.stop = async () => {
        mockStopCalls += 1
        await realStop()
      }
      return handle
    },
    spawnCypress: (opts) => {
      capturedEnv = opts.env
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(mockStopCalls, 0, 'mock must not be stopped mid-batch')
      })
    },
  })

  assert.equal(code, 0)
  assert.equal(mockStarts, 1, 'Wikidata mock must start exactly once')
  assert.equal(mockStopCalls, 1, 'Wikidata mock must be stopped once at end')
  assert.equal(isPidAlive(mockPid), false)
  // The Wikidata endpoint is injected into Cypress env under its own key,
  // distinct from the OpenAI endpoint key, and the wrapper owns the lifetime.
  assert.equal(
    capturedEnv && capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
    '1'
  )
  assert.ok(
    capturedEnv && capturedEnv[E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY]
  )
  assert.ok(
    Number.isInteger(
      Number(capturedEnv && capturedEnv[E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY])
    )
  )
  // The OpenAI endpoint key is NOT set for a Wikidata-only batch.
  assert.equal(
    capturedEnv && capturedEnv[E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY],
    undefined
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('private-mock startup failure: cleans partial owned work (mock + SUT) and returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressSpawned = false

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      throw new Error('private mock startup failed')
    },
    spawnCypress: () => {
      cypressSpawned = true
      return makeCypressChild(0)
    },
  })

  assert.equal(code, 1)
  assert.equal(
    cypressSpawned,
    false,
    'Cypress must not run after mock startup failure'
  )
  state.owned = await waitForOwnedPids(standIn.pidsFile)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('unexpected mock exit during Cypress run: terminates Cypress + settles owned services with failure', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressChild = null
  let mockPid = 0

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      return handle
    },
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(isPidAlive(state.owned.leader), true)
        // Simulate the private mock exiting unexpectedly during the run.
        process.kill(mockPid, 'SIGKILL')
      })
      return cypressChild
    },
  })

  assert.equal(code, 1, 'mock exit must end the run with visible failure')
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop after the mock exit'
  )
  assert.equal(isPidAlive(mockPid), false)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('cancel during a mock-requiring batch: mock + SUT are stopped as part of invocation cleanup', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let mockPid = 0

  const code = await runE2eBatch({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      return handle
    },
    spawnCypress: () =>
      makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(mockPid), true)
        cancel.trigger()
      }),
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.equal(isPidAlive(mockPid), false, 'mock must be stopped on cancel')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('missing explicit --spec refuses before the stack starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  let startCalled = false

  const code = await runE2eBatch({
    argv: [],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1)
  assert.equal(startCalled, false)
})

test('cancel during readiness-wait: stops owned descendants and returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let cypressSpawned = false

  // Trigger cancellation only once the owned tree has published its pids, so
  // the test observes a live tree before the readiness wait is aborted.
  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: async () => {
        if (existsSync(standIn.pidsFile)) {
          if (state.owned.leader === 0) {
            state.owned = JSON.parse(readFileSync(standIn.pidsFile, 'utf8'))
            cancel.trigger()
          }
        }
        return {
          ok: false,
          tcpResults: [],
          readinessResult: { ok: false },
          exitCode: 1,
        }
      },
      timeoutMs: 30_000,
    }),
    spawnCypress: () => {
      cypressSpawned = true
      return makeCypressChild(0)
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.equal(
    cypressSpawned,
    false,
    'Cypress must not run after readiness cancel'
  )
  assert.ok(state.owned.leader > 0, 'owned tree was alive when cancelled')
  // Completion waits for bounded escalation: the owned tree is actually gone,
  // not merely signalled.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Ownership is released only after the owned tree is stopped.
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('cancel during Cypress run: stops Cypress child and owned descendants, returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let cypressChild = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
        // Trigger cancellation while the Cypress child is running.
        cancel.trigger()
      })
      return cypressChild
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  // The Cypress child (the runner) was stopped first by the cancellation.
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop'
  )
  // Completion waits for bounded escalation: owned tree actually gone.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Ownership released only after the owned tree is stopped.
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('cancel during Cypress run: a foreign peer survives while the owned leader+grandchild are reaped', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  const foreign = spawnForeignProcess()
  t.after(() => {
    try {
      foreign.kill('SIGKILL')
    } catch {
      // already gone
    }
  })

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        cancel.trigger()
      }),
    cancel,
  })

  assert.equal(code, 1)
  // Representative child/grandchild exit: owned leader and grandchild reaped.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Peer survival: the foreign process is NOT signalled by the batch.
  assert.equal(
    isPidAlive(foreign.pid),
    true,
    'foreign peer must survive cancellation'
  )
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

test('cleanup failure remains visible: a failing shutdown is not masked as success', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const cancel = manualCancel()
  const throwingLifetime = {
    child: { kill: () => undefined, pid: 0 },
    target: {},
    ready: Promise.resolve({ ok: true, exitCode: 0 }),
    shutdown: async () => {
      throw new Error('shutdown failed: owned tree did not exit')
    },
  }

  await assert.rejects(
    runE2eBatch({
      argv: cypressArgv(),
      checkoutRoot: checkout.root,
      startLifetime: async () => throwingLifetime,
      spawnCypress: () =>
        makeLiveCypressChild(() => {
          cancel.trigger()
        }),
      cancel,
    }),
    /shutdown failed/,
    'a cleanup failure must propagate rather than be swallowed into success'
  )
})

test('required service exits during Cypress run: terminates Cypress, settles owned tree, returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressChild = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
        // Simulate a required application service exiting during the run.
        process.kill(state.owned.leader, 'SIGKILL')
      })
      return cypressChild
    },
  })

  assert.equal(code, 1, 'service exit must end the run with visible failure')
  // Prompt failure observation: Cypress was signalled to stop in response to
  // the service exit, not left running to finish on its own.
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop after the service exit'
  )
  // Settled cleanup: owned leader+grandchild reaped, ownership released.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('required service exits between readiness and run: timing window still ends with failure + cleanup', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressChild = null
  let leaderKilled = false

  // Kill the leader during the first successful healthcheck — i.e. right as
  // readiness resolves — so the exit lands in the window between readiness
  // and the Cypress-run observer attachment. The healthcheck only reports
  // ok=true AFTER the leader is killed, so readiness and the exit coincide.
  const healthcheckFn = async () => {
    if (!leaderKilled) {
      try {
        const pids = JSON.parse(readFileSync(standIn.pidsFile, 'utf8'))
        if (Number.isInteger(pids.leader) && pids.leader > 0) {
          state.owned = pids
          process.kill(pids.leader, 'SIGKILL')
          leaderKilled = true
        }
      } catch {
        // pids file not written yet
      }
    }
    if (!leaderKilled) {
      return {
        ok: false,
        tcpResults: [],
        readinessResult: { ok: false },
        exitCode: 1,
      }
    }
    return {
      ok: true,
      tcpResults: [],
      readinessResult: { ok: true },
      exitCode: 0,
    }
  }

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn, { healthcheckFn }),
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        if (state.owned.leader === 0) {
          state.owned = await waitForOwnedPids(standIn.pidsFile)
        }
      })
      return cypressChild
    },
  })

  assert.equal(code, 1, 'timing-window exit must end with visible failure')
  assert.ok(leaderKilled, 'leader was killed during readiness')
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop after the timing-window exit'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('service exit during Cypress run: a foreign peer survives while owned leader+grandchild are reaped', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const foreign = spawnForeignProcess()
  t.after(() => {
    try {
      foreign.kill('SIGKILL')
    } catch {
      // already gone
    }
  })

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        process.kill(state.owned.leader, 'SIGKILL')
      }),
  })

  assert.equal(code, 1)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(
    isPidAlive(foreign.pid),
    true,
    'foreign peer must survive the service-exit shutdown'
  )
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

test('service exit during Cypress run: Cypress stdio is preserved (not suppressed)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let capturedStdio = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    stdio: 'inherit',
    spawnCypress: (opts) => {
      capturedStdio = opts.stdio
      return makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        process.kill(state.owned.leader, 'SIGKILL')
      })
    },
  })

  assert.equal(code, 1)
  assert.equal(
    capturedStdio,
    'inherit',
    'Cypress stdio must be inherited (diagnostics preserved), not suppressed'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

/**
 * A Cypress child that simulates `cypress open` (Electron) behaviour on
 * cancellation: it does NOT exit on SIGTERM (Electron prints a graceful-exit
 * message and keeps running), and only exits on SIGKILL. Used to verify the
 * wrapper's bounded SIGTERM→SIGKILL escalation on the cancellation path.
 */
function makeStickyCypressChild(onSpawn) {
  const child = new EventEmitter()
  child.pid = 0
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.killed = false
  child.kill = (signal) => {
    if (child.killed) return
    if (signal === 'SIGKILL') {
      child.killed = signal
      queueMicrotask(() => child.emit('exit', null, child.killed))
    }
    // SIGTERM (and any other signal) is ignored: Electron does not exit on it.
  }
  child.unref = () => undefined
  if (onSpawn) {
    queueMicrotask(async () => {
      try {
        await onSpawn()
      } catch {
        // observation best-effort
      }
    })
  }
  return child
}

/**
 * A Cypress `open` (interactive) child that stays alive until `.close()` is
 * called (simulating the developer closing Cypress, which exits cleanly with
 * code 0) or `.kill(signal)` is called (simulating cancellation/service-exit
 * termination). `onSpawn` runs once the "session" is open, so the test can
 * simulate spec selections/reruns and then close the session.
 */
function makeOpenCypressChild(onSpawn) {
  const child = new EventEmitter()
  child.pid = 0
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.killed = false
  child.kill = (signal) => {
    if (child.killed) return
    child.killed = signal || 'SIGTERM'
    queueMicrotask(() => child.emit('exit', null, child.killed))
  }
  child.close = () => {
    if (child.killed) return
    child.killed = true
    queueMicrotask(() => child.emit('exit', 0, null))
  }
  child.unref = () => undefined
  queueMicrotask(async () => {
    try {
      await onSpawn?.(child)
    } catch {
      // observation best-effort; do not block the exit
    }
  })
  return child
}

test('interactive session: one owned stack shared across selections/reruns, no after-spec teardown, cleanup on close', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eInteractive({
    argv: [], // pure browsing: no preselected spec, no mock
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeOpenCypressChild(async (session) => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        // "Selection" (first spec run within the open session): stack alive.
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
        // "Rerun" (second spec run): the SAME stack is still alive — no
        // after-spec teardown happened between selections.
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
        // Closing Cypress ends the session.
        session.close()
      }),
  })

  assert.equal(code, 0, 'a clean close must return 0')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('interactive session: owned mock stays alive across reruns (no after-spec teardown), stopped on close', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let mockStarts = 0
  let mockStopCalls = 0
  let mockPid = 0

  const code = await runE2eInteractive({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      mockStarts += 1
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      const realStop = handle.stop
      handle.stop = async () => {
        mockStopCalls += 1
        await realStop()
      }
      return handle
    },
    spawnCypress: () =>
      makeOpenCypressChild(async (session) => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        // First selection of the mock-requiring spec: mock + stack alive.
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(mockStopCalls, 0, 'mock must not be stopped after a spec')
        // Rerun: the SAME mock + stack are still alive — no after-spec teardown.
        assert.equal(isPidAlive(mockPid), true)
        assert.equal(
          mockStopCalls,
          0,
          'mock must not be stopped between reruns'
        )
        session.close()
      }),
  })

  assert.equal(code, 0)
  assert.equal(mockStarts, 1, 'mock must start exactly once for the session')
  assert.equal(mockStopCalls, 1, 'mock must be stopped once on close')
  assert.equal(isPidAlive(mockPid), false)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('interactive session: endpoint configuration verified before browser launch (owned endpoint injected into Cypress env)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let capturedEnv = null
  let mockEndpoint = null

  const code = await runE2eInteractive({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      const handle = spawnIdlePrivateMockHandle()
      mockEndpoint = handle.endpoint
      return handle
    },
    spawnCypress: (opts) => {
      capturedEnv = opts.env
      return makeOpenCypressChild(async (session) => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        session.close()
      })
    },
  })

  assert.equal(code, 0)
  assert.ok(capturedEnv, 'Cypress env must be supplied before browser launch')
  assert.equal(
    capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
    '1',
    'plugin must be in adapter mode (wrapper owns the lifetime)'
  )
  const injected = JSON.parse(capturedEnv[E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY])
  assert.deepEqual(
    injected,
    mockEndpoint,
    'owned endpoint injected before launch'
  )
  assert.ok(
    Number.isInteger(Number(capturedEnv[E2E_RUNNER_MOCK_PGID_ENV_KEY])),
    'owned mock pgid injected before launch'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

test('interactive session: unsupported preselected spec refuses before the stack starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  let startCalled = false

  const code = await runE2eInteractive({
    argv: ['--spec', 'e2e_test/features/book_reading/epub_book.feature'],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeOpenCypressChild(),
  })

  assert.equal(code, 1)
  assert.equal(startCalled, false, 'must refuse before starting the stack')
})

test('interactive session: cancellation stops Cypress + stack + mock, returns nonzero, zero survivors', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let mockPid = 0
  let openChild = null

  const code = await runE2eInteractive({
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      const handle = spawnIdlePrivateMockHandle()
      mockPid = handle.child.pid
      return handle
    },
    spawnCypress: () => {
      openChild = makeOpenCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(mockPid), true)
        cancel.trigger()
      })
      return openChild
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.ok(
    openChild && openChild.killed,
    'Cypress child must be signalled to stop'
  )
  assert.equal(isPidAlive(mockPid), false, 'mock must be stopped on cancel')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('interactive session: required service exit terminates the session + cleanup, returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let openChild = null

  const code = await runE2eInteractive({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () => {
      openChild = makeOpenCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        // Simulate a required application service exiting during the session.
        process.kill(state.owned.leader, 'SIGKILL')
      })
      return openChild
    },
  })

  assert.equal(
    code,
    1,
    'service exit must end the session with visible failure'
  )
  assert.ok(
    openChild && openChild.killed,
    'Cypress child must be signalled to stop after the service exit'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('interactive spawn: `cypress open` is invoked WITHOUT `--spec` (spec selection happens in the Cypress UI)', () => {
  const captured = []
  const fakeChild = new EventEmitter()
  fakeChild.pid = 0
  fakeChild.kill = () => undefined
  fakeChild.unref = () => undefined
  const spawnFn = (cmd, args, opts) => {
    captured.push({ cmd, args, opts })
    return fakeChild
  }

  // The wrapper still passes `specs` through to the spawner (so the caller's
  // shape is stable), but `cypress open` must NOT receive `--spec` — even when
  // a preselected spec was supplied as a resource-requirement hint upstream.
  defaultSpawnCypressOpen({
    specs: [SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC],
    cwd: '/repo',
    env: {},
    cypressBin: '/repo/node_modules/cypress/bin/cypress',
    configFile: 'e2e_test/config/ci.ts',
    stdio: 'inherit',
    spawnFn,
  })

  assert.equal(captured.length, 1, 'spawn must be invoked exactly once')
  const { args } = captured[0]
  assert.equal(args[0], '/repo/node_modules/cypress/bin/cypress')
  assert.equal(args[1], 'open', 'first Cypress arg is the open mode')
  assert.equal(
    args.includes('--spec'),
    false,
    '--spec must NOT be forwarded to cypress open'
  )
  assert.equal(args.includes('--e2e'), true, '--e2e is forwarded')
  assert.ok(
    args.includes('--config-file') && args.includes('e2e_test/config/ci.ts'),
    '--config-file is forwarded'
  )
})

test('interactive cancel: SIGTERM-ignoring Cypress child is escalated to SIGKILL, then owned tree is cleaned with zero survivors', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let stickyChild = null
  const killSignals = []

  const code = await runE2eInteractive({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    cancelEscalationMs: 50,
    spawnCypress: () => {
      stickyChild = makeStickyCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
        cancel.trigger()
      })
      const realKill = stickyChild.kill
      stickyChild.kill = (signal) => {
        killSignals.push(signal)
        return realKill.call(stickyChild, signal)
      }
      return stickyChild
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.deepEqual(
    killSignals,
    ['SIGTERM', 'SIGKILL'],
    'wrapper must escalate SIGTERM → SIGKILL when the Cypress child ignores SIGTERM'
  )
  assert.ok(stickyChild && stickyChild.killed === 'SIGKILL')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('normal close path is unaffected: no spurious SIGKILL when Cypress exits on its own without cancellation', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const killSignals = []

  const code = await runE2eInteractive({
    argv: [],
    ...isolatedLifetimeOpts(checkout.root, standIn),
    cancelEscalationMs: 50,
    spawnCypress: () => {
      const child = makeOpenCypressChild(async (session) => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        // Developer closes Cypress cleanly — no cancellation triggered.
        session.close()
      })
      const realKill = child.kill
      child.kill = (signal) => {
        killSignals.push(signal)
        return realKill.call(child, signal)
      }
      return child
    },
  })

  assert.equal(code, 0, 'a clean close must return 0 with no cancellation')
  assert.deepEqual(killSignals, [], 'no signal must be sent on a clean close')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

// ---------------------------------------------------------------------------
// Slice 8: primary-target batches (unconfigured checkout, canonical endpoints)
// ---------------------------------------------------------------------------
//
// An unconfigured primary checkout (no `.worktree.local.json`, not a linked
// worktree) uses canonical endpoints (5173/5174/9081, `doughnut_e2e_test`)
// with services owned by the invocation. The wrapper detects the primary
// target via the existing `resolveSutCheckoutTarget` rules, refuses foreign
// listeners on canonical application ports without adoption or signalling,
// and stops the spawned supervisor child on shutdown via the same
// owned-tree termination used by the isolated target (no separate shutdown
// algorithm). Primary mocks use the canonical Mountebank port (2525).

/**
 * Lifetime options for a primary-target batch. The primary target has no
 * isolated allocation, so the stand-in does not bind real canonical ports;
 * `isPortOccupiedFn: async () => false` bypasses the real port check for the
 * happy path (foreign-listener refusal is exercised separately with real
 * listeners on ephemeral ports via a custom `runtimeTarget`).
 */
function primaryLifetimeOpts(checkoutRoot, standIn, extra = {}) {
  return {
    checkoutRoot,
    spawnFn: standIn.spawnFn,
    logFile: path.join(checkoutRoot, 'sut.log'),
    pidFile: path.join(checkoutRoot, 'sut.pid'),
    timeoutMs: extra.timeoutMs ?? 5_000,
    pollMs: extra.pollMs ?? 50,
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: extra.healthcheckFn ?? healthyOnce,
    isPortOccupiedFn: async () => false,
  }
}

test('primary batch: unconfigured target → canonical endpoints → SUT owned by invocation → Cypress runs → cleanup → zero surviving owned processes', async (t) => {
  const checkout = makePrimaryCheckout(t)
  // No writeIsolatedConfig: this is an unconfigured primary checkout.
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
      }),
  })

  assert.equal(code, 0)
  // Same lifecycle as the isolated target: the owned supervisor child +
  // grandchild are reaped via the shared owned-tree termination.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Primary target claims no SUT ownership (no owner lock dir is created).
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('primary batch: shared MySQL/Redis and Development preserved — a foreign peer survives, no SUT owner lock is created', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const foreign = spawnForeignProcess()
  t.after(() => {
    try {
      foreign.kill('SIGKILL')
    } catch {
      // already gone
    }
  })

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
      }),
  })

  assert.equal(code, 0)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Shared infrastructure / foreign peers are not signalled by the batch.
  assert.equal(isPidAlive(foreign.pid), true, 'foreign peer must survive')
  // No SUT ownership is claimed on the primary target.
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

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

test('primary batch: same lifecycle as isolated — primary shutdown stops the spawned supervisor child (no separate algorithm)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  // A nonzero Cypress result still settles the owned tree via the same
  // shutdown path (stopOwnedSutProcessTree), proving the primary target
  // reuses the isolated target's owned-tree termination.
  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(3, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }),
  })

  assert.equal(code, 3, 'must preserve the nonzero Cypress exit code')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
})

test('primary path: mock-requiring spec does NOT start a wrapper-owned mock (approved null, SUT Mountebank serves the spec)', async (t) => {
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
    'primary path must not start a wrapper-owned mock even for the mock-requiring spec'
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

// ---------------------------------------------------------------------------
// Slice 9: built-asset-target batches (prepared frontend/CLI/MCP bundles,
// no Vite dev server — the built frontend is served statically by the local LB)
// ---------------------------------------------------------------------------
//
// The built target is distinguished from dev/isolated/primary targets purely
// by launch data: `target.built === true` omits the Vite upstream env, the
// `frontend:sut` service, the Vite TCP readiness check, and the Vite port in
// foreign-listener refusal. It reuses the SAME `startOwnedSutLifetime` and
// `lifetime.shutdown()` / owned-tree termination — no separate lifecycle.

const BUILT_RUNTIME_TARGET = { ...LEGACY_SUT_RUNTIME_TARGET, built: true }

test('built target launch data: sutServiceArgs omits frontend:sut and uses local:lb (no Vite)', () => {
  const args = sutServiceArgs(BUILT_RUNTIME_TARGET)
  assert.equal(
    args.includes('frontend:sut'),
    false,
    'must not start Vite dev server'
  )
  assert.equal(args.includes('local:lb'), true, 'must start the local LB')
  assert.equal(
    args.includes('local:lb:vite'),
    false,
    'must use local:lb not local:lb:vite'
  )
  assert.equal(args.includes('backend:sut'), true, 'must start the backend')
  assert.equal(
    args.includes('start:mb'),
    true,
    'must start Mountebank (canonical port)'
  )
})

test('built target launch data: runtimeTargetProcessEnv omits Vite upstream + dev port', () => {
  const env = runtimeTargetProcessEnv(BUILT_RUNTIME_TARGET)
  assert.equal(
    env.LOCAL_LB_VITE_UPSTREAM,
    undefined,
    'no Vite upstream for built target'
  )
  assert.equal(
    env.FRONTEND_DEV_PORT,
    undefined,
    'no Vite dev port for built target'
  )
  assert.equal(env.SERVER_PORT, '9081', 'backend port preserved')
  assert.equal(
    env.LOCAL_LB_BACKEND,
    'http://127.0.0.1:9081',
    'LB backend preserved'
  )
  assert.equal(env.LOCAL_LB_LISTEN_PORT, '5173', 'LB listen port preserved')
  assert.equal(
    env.FRONTEND_BACKEND_ORIGIN,
    'http://127.0.0.1:9081',
    'backend origin preserved'
  )
})

test('built target launch data: healthEndpoints omits the frontend vite TCP check', () => {
  const endpoints = healthEndpoints(BUILT_RUNTIME_TARGET)
  const services = endpoints.tcpChecks.map((c) => c.service)
  assert.equal(
    services.includes('frontend vite'),
    false,
    'no Vite readiness check for built target'
  )
  assert.equal(services.includes('backend'), true, 'backend check preserved')
  assert.equal(services.includes('local LB'), true, 'LB check preserved')
  assert.equal(
    services.includes('mountebank'),
    true,
    'Mountebank check preserved'
  )
  assert.equal(
    endpoints.readinessUrl,
    'http://127.0.0.1:5173/__lb__/ready',
    'readiness URL preserved (LB + backend, no Vite)'
  )
})

test('built target launch data: foreign-listener refusal skips the Vite port', async () => {
  // A foreign listener on the Vite port must NOT trigger refusal for a built
  // target (the Vite port is not used). A foreign listener on the LB port
  // still must.
  const freeVite = await allocateFreePort()
  const freeLb = await allocateFreePort()
  const builtTarget = {
    backendPort: 9081,
    vitePort: freeVite,
    lbListenPort: freeLb,
    mountebankPort: 2525,
    built: true,
  }
  // Vite port occupied → not reported (built target skips it).
  const occupiedNoVite = await listOccupiedApplicationPorts(
    builtTarget,
    async (port) => port === freeVite
  )
  assert.deepEqual(
    occupiedNoVite,
    [],
    'built target must not report a foreign Vite listener'
  )
  // LB port occupied → reported (built target still checks backend + LB).
  const occupiedLb = await listOccupiedApplicationPorts(
    builtTarget,
    async (port) => port === freeLb
  )
  assert.deepEqual(
    occupiedLb,
    [`local LB ${freeLb}`],
    'built target must report a foreign LB listener'
  )
})

test('built-asset batch: target arguments select the built target → SUT starts without Vite → readiness → Cypress runs → cleanup → zero survivors', async (t) => {
  const checkout = makePrimaryCheckout(t)
  // No writeIsolatedConfig: this is an unconfigured primary checkout with
  // prepared bundles (built-asset target).
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let capturedHealthcheckTarget = null
  let capturedSpecs = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn, {
      // Capture the runtimeTarget that reaches the healthcheck — it must
      // carry built: true, proving the built target flows through the
      // existing resolveSutCheckoutTarget → startOwnedSutLifetime →
      // waitForSutHealthy path. Readiness does NOT wait for a Vite listener
      // (the built target has no Vite dev server).
      healthcheckFn: async (hcOpts) => {
        capturedHealthcheckTarget = hcOpts.runtimeTarget
        return {
          ok: true,
          tcpResults: [],
          readinessResult: { ok: true },
          exitCode: 0,
        }
      },
    }),
    runtimeTarget: BUILT_RUNTIME_TARGET,
    spawnCypress: (opts) => {
      capturedSpecs = opts.specs
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
      })
    },
  })

  assert.equal(code, 0)
  // Target arguments forwarded correctly: the built target reaches the
  // healthcheck with built: true (distinguished from dev/isolated/primary).
  assert.equal(
    capturedHealthcheckTarget?.built,
    true,
    'built target forwarded to healthcheck'
  )
  // Selection forwarding: selected specs reach Cypress.
  assert.ok(
    capturedSpecs && capturedSpecs.length === 1,
    'one selected spec forwarded to Cypress'
  )
  assert.equal(
    capturedSpecs[0],
    SUPPORTED_ISOLATED_CYPRESS_SPEC,
    'the selected supported spec is forwarded to Cypress'
  )
  // Cleanup: zero surviving owned processes.
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('built-asset batch: nonzero Cypress result still settles the owned tree (same lifecycle)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    runtimeTarget: BUILT_RUNTIME_TARGET,
    spawnCypress: () =>
      makeCypressChild(7, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
      }),
  })

  assert.equal(code, 7, 'must preserve the nonzero Cypress exit code')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('built-asset batch: cancellation stops Cypress + owned tree, returns nonzero, zero survivors', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()
  let cypressChild = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    runtimeTarget: BUILT_RUNTIME_TARGET,
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        cancel.trigger()
      })
      return cypressChild
    },
    cancel,
  })

  assert.equal(code, 1, 'cancellation must return a visible nonzero outcome')
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('built-asset batch: required service exit terminates Cypress + settles owned tree, returns nonzero', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let cypressChild = null

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...primaryLifetimeOpts(checkout.root, standIn),
    runtimeTarget: BUILT_RUNTIME_TARGET,
    spawnCypress: () => {
      cypressChild = makeLiveCypressChild(async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        // Simulate a required application service exiting during the run.
        process.kill(state.owned.leader, 'SIGKILL')
      })
      return cypressChild
    },
  })

  assert.equal(code, 1, 'service exit must end the run with visible failure')
  assert.ok(
    cypressChild && cypressChild.killed,
    'Cypress child must be signalled to stop'
  )
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

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

// ---------------------------------------------------------------------------
// Slice 10: route CI E2E matrix jobs through the owned invocation wrapper.
// The primary path accepts any --spec (no allowlist); the isolated path still
// enforces the allowlist. --browser is forwarded to `cypress run`.
// ---------------------------------------------------------------------------

test('primary path: multi-line glob --spec accepted without throwing, joined --spec contains both globs comma-separated', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let capturedSpecs = null

  const code = await runE2eBatch({
    argv: ['--spec', 'e2e_test/features/foo/**\ne2e_test/features/bar/**'],
    ...primaryLifetimeOpts(checkout.root, standIn),
    spawnCypress: (opts) => {
      capturedSpecs = opts.specs
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
      })
    },
  })

  assert.equal(code, 0)
  assert.ok(capturedSpecs, 'specs must be forwarded to Cypress')
  assert.equal(capturedSpecs.length, 2, 'multi-line glob splits into two specs')
  assert.deepEqual(
    capturedSpecs,
    ['e2e_test/features/foo/**', 'e2e_test/features/bar/**'],
    'both globs preserved'
  )
  assert.equal(
    capturedSpecs.join(','),
    'e2e_test/features/foo/**,e2e_test/features/bar/**',
    'joined --spec is comma-separated'
  )
})

test('primary path: no private mock started (approved null), Cypress env has no E2E_RUNNER_OWNS_LIFETIME', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let capturedEnv = null
  let mockStarted = false

  const code = await runE2eBatch({
    argv: ['--spec', 'e2e_test/features/foo/**\ne2e_test/features/bar/**'],
    ...primaryLifetimeOpts(checkout.root, standIn),
    startPrivateOpenAiMockFn: async () => {
      mockStarted = true
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
  assert.equal(mockStarted, false, 'no private mock for primary path')
  assert.equal(
    capturedEnv && capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
    undefined,
    'Cypress env must not signal wrapper-owned lifetime'
  )
})

test('defaultSpawnCypress: --browser chrome forwarded as ["--browser","chrome"] in cypress run args', () => {
  const captured = []
  const fakeChild = new EventEmitter()
  fakeChild.pid = 0
  fakeChild.kill = () => undefined
  fakeChild.unref = () => undefined
  const spawnFn = (cmd, args, opts) => {
    captured.push({ cmd, args, opts })
    return fakeChild
  }

  defaultSpawnCypress({
    specs: ['e2e_test/features/foo/**'],
    cwd: '/repo',
    env: {},
    cypressBin: '/repo/node_modules/cypress/bin/cypress',
    configFile: 'e2e_test/config/ci.ts',
    stdio: 'inherit',
    browser: 'chrome',
    spawnFn,
  })

  assert.equal(captured.length, 1)
  const { args } = captured[0]
  const browserIdx = args.indexOf('--browser')
  assert.ok(browserIdx >= 0, '--browser must be in cypress run args')
  assert.equal(args[browserIdx + 1], 'chrome', 'browser value is chrome')
})

test('defaultSpawnCypress: without --browser no --browser arg is emitted', () => {
  const captured = []
  const fakeChild = new EventEmitter()
  fakeChild.pid = 0
  fakeChild.kill = () => undefined
  fakeChild.unref = () => undefined
  const spawnFn = (cmd, args, opts) => {
    captured.push({ cmd, args, opts })
    return fakeChild
  }

  defaultSpawnCypress({
    specs: ['e2e_test/features/foo/**'],
    cwd: '/repo',
    env: {},
    cypressBin: '/repo/node_modules/cypress/bin/cypress',
    configFile: 'e2e_test/config/ci.ts',
    stdio: 'inherit',
    spawnFn,
  })

  assert.equal(captured.length, 1)
  const { args } = captured[0]
  assert.equal(
    args.includes('--browser'),
    false,
    'no --browser arg without a browser'
  )
})

test('isolated path: non-allowlisted glob still refuses (allowlist enforced for isolated only)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  let startCalled = false

  const code = await runE2eBatch({
    argv: ['--spec', 'e2e_test/features/foo/**\ne2e_test/features/bar/**'],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1)
  assert.equal(
    startCalled,
    false,
    'must refuse non-allowlisted glob for isolated path'
  )
})

// ---------------------------------------------------------------------------
// Slice 13: fresh isolated worktree provisioning on first cy:run.
// The wrapper must provision a fresh linked worktree (no .worktree.local.json)
// on first `pnpm cy:run` instead of failing in resolveSutCheckoutTarget before
// startOwnedSutLifetime provisions. `isolated` is determined via
// worktreeIsolationApplies (identity file OR linked git worktree) without
// requiring a complete allocation; the pre-start resolveSutCheckoutTarget is
// skipped for isolated (startOwnedSutLifetime provisions + resolves).
// ---------------------------------------------------------------------------

/**
 * A ready lifetime stand-in for provisioning tests: the SUT is already
 * healthy and shutdown is a no-op. Used to fake `startLifetime` so the test
 * observes the wrapper reached (and provisioned via) the lifetime start
 * without spawning real services.
 */
function readyLifetimeStandIn() {
  return {
    child: { kill: () => undefined, pid: 0 },
    target: {},
    ready: Promise.resolve({ ok: true, exitCode: 0 }),
    shutdown: async () => undefined,
  }
}

test('fresh isolated worktree (no .worktree.local.json) provisions on first cy:run: startLifetime is called instead of failing in resolveSutCheckoutTarget', async (t) => {
  const checkout = makePrimaryCheckout(t)
  // No writeIsolatedConfig: simulate a fresh linked worktree with no
  // allocation. isIsolatedCheckoutFn fakes the linked-worktree topology so
  // the wrapper treats this checkout as isolated without a real git worktree.
  let startCalled = false
  const code = await runE2eBatch({
    argv: cypressArgv(),
    checkoutRoot: checkout.root,
    isIsolatedCheckoutFn: () => true,
    startLifetime: async () => {
      startCalled = true
      return readyLifetimeStandIn()
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 0)
  assert.equal(
    startCalled,
    true,
    'fresh isolated worktree must provision (startLifetime called); the wrapper must not fail in resolveSutCheckoutTarget before provisioning'
  )
})

test('primary checkout regression guard: foreign listener on a canonical port still refuses BEFORE startLifetime (resolveSutCheckoutTarget + port check run first)', async (t) => {
  const checkout = makePrimaryCheckout(t)
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
    isIsolatedCheckoutFn: () => false,
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
  assert.equal(
    startCalled,
    false,
    'primary path must call resolveSutCheckoutTarget + port check before startLifetime'
  )
  assert.equal(
    await isPortStillListening(foreignListener.port),
    true,
    'foreign listener must not be signalled'
  )
})

test('already-provisioned isolated worktree still works: startLifetime is called and re-reads the existing allocation (no re-provisioning path skipped)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  let startCalled = false
  const code = await runE2eBatch({
    argv: cypressArgv(),
    checkoutRoot: checkout.root,
    isIsolatedCheckoutFn: () => true,
    startLifetime: async () => {
      startCalled = true
      return readyLifetimeStandIn()
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 0)
  assert.equal(
    startCalled,
    true,
    'already-provisioned isolated worktree must still reach startLifetime'
  )
})
