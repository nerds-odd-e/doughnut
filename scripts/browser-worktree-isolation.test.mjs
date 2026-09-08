import assert from 'node:assert/strict'
import { writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
} from './backend-test-worktree-linked-fixtures.mjs'
import {
  assertReadersRefuseIncompleteAllocation,
  incompleteAllocation,
  isolatedCypressSpec,
  makeRestartSpies,
  malformedJson,
  runHealth,
  runStart,
  withCiEnv,
} from './browser-worktree-isolation-fixtures.mjs'
import { loadIsolatedE2eStartAllocation } from './browser-worktree-isolation.mjs'
import { guardCypressNodeSetup } from './isolated-cypress.mjs'
import {
  completeIsolatedConfig,
  identityAndPortsConfig,
  identityOnlyConfig,
} from './sut-isolated-fixtures.mjs'
import { runSutRestart } from './sut-restart.mjs'
import { makeStartSpy } from './sut-start-fixtures.mjs'

test('unconfigured primary and CI keep shared SUT and Cypress defaults', async (t) => {
  withCiEnv(t)
  const checkout = makePrimaryCheckout(t)
  const start = makeStartSpy()
  const restart = makeRestartSpies()
  const healthLogs = []
  const healthAccessed = []

  assert.equal(await runStart(checkout.root, start), 0)
  assert.equal(start.calls.length, 1)

  await runHealth(checkout.root, healthLogs, healthAccessed)
  assert.ok(healthAccessed.length > 0)
  assert.ok(healthLogs.some((line) => /TCP|HTTP readiness/.test(line)))

  await runSutRestart({
    checkoutRoot: checkout.root,
    execFileFn: restart.execFileFn,
    spawnFn: restart.spawnFn,
    log: () => undefined,
  })
  assert.ok(restart.lsofCalls.length > 0)
  assert.equal(restart.spawnCalls.length, 1)

  await guardCypressNodeSetup(checkout.root)
})

test('configured primary identity-only can start; linked checkouts without identity refuse health, restart, and Cypress', async (t) => {
  withCiEnv(t)
  const configured = makePrimaryCheckout(t, {
    config: JSON.stringify(identityOnlyConfig),
  })
  const linked = makeLinkedWorktreeCheckout(t)

  assert.equal(loadIsolatedE2eStartAllocation(configured.root).id, 'wt_a7c2')

  for (const checkout of [configured, linked]) {
    const healthLogs = []
    const healthAccessed = []
    await assert.rejects(
      runHealth(checkout.root, healthLogs, healthAccessed),
      incompleteAllocation
    )
    assert.equal(healthLogs.length, 0)
    assert.equal(healthAccessed.length, 0)

    const restart = makeRestartSpies()
    await assert.rejects(
      runSutRestart({
        checkoutRoot: checkout.root,
        execFileFn: restart.execFileFn,
        spawnFn: restart.spawnFn,
        log: () => undefined,
      }),
      incompleteAllocation
    )
    assert.equal(restart.lsofCalls.length, 0)
    assert.equal(restart.spawnCalls.length, 0)

    const hooks = { reset: false }
    await assert.rejects(async () => {
      await guardCypressNodeSetup(checkout.root)
      hooks.reset = true
    }, isolatedCypressSpec)
    assert.equal(hooks.reset, false)
  }
})

test('identity and ports without E2E database still refuse health, restart, and Cypress', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(identityAndPortsConfig),
  })
  assert.deepEqual(
    loadIsolatedE2eStartAllocation(checkout.root).e2e,
    identityAndPortsConfig.e2e
  )
  await assertReadersRefuseIncompleteAllocation(checkout.root)
})

test('present invalid E2E database refuses start, health, restart, and Cypress readers', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify({
      id: 'wt_a7c2',
      e2e: {
        ...identityAndPortsConfig.e2e,
        database: 'invalid-name',
      },
    }),
  })
  await assertReadersRefuseIncompleteAllocation(checkout.root, {
    refuseStartAllocation: true,
  })
})

test('present invalid e2e container refuses start, health, restart, and Cypress readers', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify({
      id: 'wt_a7c2',
      e2e: null,
    }),
  })
  await assertReadersRefuseIncompleteAllocation(checkout.root, {
    refuseStartAllocation: true,
  })
})

test('malformed isolation JSON refuses clearly before shared-state effects', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeFileSync(path.join(checkout.root, '.worktree.local.json'), '{"id":')

  const start = makeStartSpy()
  await assert.rejects(runStart(checkout.root, start), malformedJson)
  assert.equal(start.calls.length, 0)

  const healthLogs = []
  const healthAccessed = []
  await assert.rejects(
    runHealth(checkout.root, healthLogs, healthAccessed),
    malformedJson
  )
  assert.equal(healthLogs.length, 0)
  assert.equal(healthAccessed.length, 0)

  const restart = makeRestartSpies()
  await assert.rejects(
    runSutRestart({
      checkoutRoot: checkout.root,
      execFileFn: restart.execFileFn,
      spawnFn: restart.spawnFn,
      log: () => undefined,
    }),
    malformedJson
  )
  assert.equal(restart.lsofCalls.length, 0)
  assert.equal(restart.spawnCalls.length, 0)

  const hooks = { reset: false }
  await assert.rejects(async () => {
    await guardCypressNodeSetup(checkout.root)
    hooks.reset = true
  }, malformedJson)
  assert.equal(hooks.reset, false)
})

test('complete isolated allocation without a live owner refuses restart before signals', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  const restart = makeRestartSpies()
  await assert.rejects(
    runSutRestart({
      checkoutRoot: checkout.root,
      execFileFn: restart.execFileFn,
      spawnFn: restart.spawnFn,
      log: () => undefined,
    }),
    /verified live SUT owner|stale or unverifiable/i
  )
  assert.equal(restart.lsofCalls.length, 0)
  assert.equal(restart.spawnCalls.length, 0)
})
