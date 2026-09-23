import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnForeignProcess,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import {
  makeLiveCypressChild,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  isolatedLifetimeOpts,
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

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
