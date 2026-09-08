import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
} from './backend-test-worktree-linked-fixtures.mjs'
import { guardCypressNodeSetup } from './browser-worktree-isolation.mjs'
import { runSutHealthcheck } from './sut-healthcheck.mjs'
import { runSutRestart } from './sut-restart.mjs'
import { healthyOnce, makeMockChild } from './sut-start-fixtures.mjs'
import { runSutStart } from './sut-start.mjs'

const isolatedRefusal = /not supported yet/i
const malformedJson = /not valid JSON/i

function makeStartSpy() {
  const calls = []
  const child = makeMockChild(4242)
  return {
    calls,
    spawnFn: (...args) => {
      calls.push(args)
      return child
    },
  }
}

function makeRestartSpies() {
  const lsofCalls = []
  const spawnCalls = []
  return {
    lsofCalls,
    spawnCalls,
    execFileFn: (cmd, args, cb) => {
      lsofCalls.push({ cmd, args })
      cb({ code: 1 }, '')
    },
    spawnFn: (...args) => {
      spawnCalls.push(args)
      const child = new EventEmitter()
      queueMicrotask(() => child.emit('close', 0))
      return child
    },
  }
}

function trackingHealthChecks(accessed) {
  return [
    {
      get service() {
        accessed.push('service')
        return 'mountebank'
      },
      host: '127.0.0.1',
      port: 1,
    },
  ]
}

async function runStart(checkoutRoot, spawn) {
  return runSutStart({
    checkoutRoot,
    spawnFn: spawn.spawnFn,
    logFile: path.join(checkoutRoot, 'sut.log'),
    pidFile: path.join(checkoutRoot, 'sut.pid'),
    timeoutMs: 5_000,
    pollMs: 50,
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: healthyOnce,
  })
}

async function runHealth(checkoutRoot, logs, accessed) {
  return runSutHealthcheck({
    checkoutRoot,
    tcpChecks: trackingHealthChecks(accessed),
    log: (line) => logs.push(line),
  })
}

function runCypressNodeSetup(checkoutRoot) {
  const hooks = { reset: false, mocks: false }
  guardCypressNodeSetup(checkoutRoot)
  hooks.reset = true
  hooks.mocks = true
  return hooks
}

function withCiEnv(t) {
  const previous = process.env.CI
  process.env.CI = 'true'
  t.after(() => {
    if (previous === undefined) {
      delete process.env.CI
    } else {
      process.env.CI = previous
    }
  })
}

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

  const cypressHooks = runCypressNodeSetup(checkout.root)
  assert.equal(cypressHooks.reset, true)
})

test('configured primary and linked checkouts refuse before shared-state effects', async (t) => {
  withCiEnv(t)
  const configured = makePrimaryCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const linked = makeLinkedWorktreeCheckout(t)

  for (const checkout of [configured, linked]) {
    const start = makeStartSpy()
    await assert.rejects(runStart(checkout.root, start), isolatedRefusal)
    assert.equal(start.calls.length, 0)

    const healthLogs = []
    const healthAccessed = []
    await assert.rejects(
      runHealth(checkout.root, healthLogs, healthAccessed),
      isolatedRefusal
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
      isolatedRefusal
    )
    assert.equal(restart.lsofCalls.length, 0)
    assert.equal(restart.spawnCalls.length, 0)

    const hooks = { reset: false, mocks: false }
    assert.throws(() => {
      guardCypressNodeSetup(checkout.root)
      hooks.reset = true
      hooks.mocks = true
    }, isolatedRefusal)
    assert.equal(hooks.reset, false)
    assert.equal(hooks.mocks, false)
  }
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

  const hooks = { reset: false, mocks: false }
  assert.throws(() => {
    guardCypressNodeSetup(checkout.root)
    hooks.reset = true
    hooks.mocks = true
  }, malformedJson)
  assert.equal(hooks.reset, false)
})
