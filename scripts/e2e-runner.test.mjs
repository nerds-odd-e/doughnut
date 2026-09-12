import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnForeignProcess,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
} from './isolated-cypress-spec-selection.mjs'
import { spawnIdlePrivateMockHandle } from './isolated-openai-mock-test-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import { healthyOnce, neverHealthy } from './sut-start-fixtures.mjs'
import {
  E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_MOCK_PGID_ENV_KEY,
  E2E_RUNNER_OWNS_LIFETIME_ENV_KEY,
} from './isolated-cypress.mjs'
import {
  defaultSpawnCypressOpen,
  runE2eBatch,
  runE2eInteractive,
} from './e2e-runner.mjs'

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

test('batch lifetime: start → run → shutdown returns Cypress outcome', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await runE2eBatch({
    argv: cypressArgv(),
    ...isolatedLifetimeOpts(checkout.root, standIn),
    spawnCypress: () =>
      makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(state.owned.grandchild), true)
      }),
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
    argv: [
      '--spec',
      'e2e_test/features/note_creation_and_update/note_creation.feature',
    ],
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
    argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
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
    argv: [
      '--spec',
      'e2e_test/features/note_creation_and_update/note_creation.feature',
    ],
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
