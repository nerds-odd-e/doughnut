import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import { runE2eHold } from './e2e-hold.mjs'
import { withEnv } from './browser-worktree-isolation-fixtures.mjs'
import { manualCancel } from './e2e-runner-cypress-fixtures.mjs'
import {
  isolatedLifetimeOpts,
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

function holdWith(checkout, standIn, cancel, onReady, extra = {}) {
  return runE2eHold({
    ...isolatedLifetimeOpts(checkout.root, standIn),
    ...extra,
    cancel,
    log: (line) => {
      if (line.includes('held')) onReady()
    },
  })
}

test('hold: the owned stack stays up until interrupted, then stops, releases ownership, and exits 0', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()

  const code = await holdWith(checkout, standIn, cancel, async () => {
    state.owned = await waitForOwnedPids(standIn.pidsFile)
    assert.equal(isPidAlive(state.owned.leader), true)
    assert.equal(isPidAlive(state.owned.grandchild), true)
    cancel.trigger()
  })

  assert.equal(code, 0, 'a clean interrupt must return 0')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('hold: a required service exiting during the hold ends it nonzero and stops the rest', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await holdWith(checkout, standIn, manualCancel(), async () => {
    state.owned = await waitForOwnedPids(standIn.pidsFile)
    process.kill(state.owned.leader, 'SIGKILL')
  })

  assert.equal(code, 1, 'service exit must end the hold with visible failure')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

for (const { name, extra, expected } of [
  { name: 'withholds', extra: {}, expected: undefined },
  {
    name: 'with paid OpenAI passes',
    extra: { paidOpenAi: true },
    expected: 'shell-openai-token',
  },
]) {
  test(`hold: ${name} the shell's OpenAI token to the services`, async (t) => {
    const checkout = makePrimaryCheckout(t)
    writeIsolatedConfig(checkout.root)
    const standIn = spawnOwnedTreeStandIn(checkout.root)
    const state = { owned: { leader: 0, grandchild: 0 } }
    trackOwnedTree(t, () => state.owned)
    withEnv(t, 'OPENAI_API_TOKEN', 'shell-openai-token')
    const serviceEnvs = []
    const recordingStandIn = {
      ...standIn,
      spawnFn: (cmd, args, opts) => {
        serviceEnvs.push(opts.env)
        return standIn.spawnFn(cmd, args, opts)
      },
    }
    const cancel = manualCancel()

    const code = await holdWith(
      checkout,
      recordingStandIn,
      cancel,
      async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        cancel.trigger()
      },
      extra
    )

    assert.equal(code, 0)
    assert.equal(serviceEnvs.length, 1)
    assert.equal(serviceEnvs[0].OPENAI_API_TOKEN, expected)
  })
}
