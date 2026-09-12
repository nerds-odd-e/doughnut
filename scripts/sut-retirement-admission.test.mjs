import assert from 'node:assert/strict'
import { existsSync, mkdirSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  completeIsolatedConfig,
  recordingMysql,
  runConfiguredStart,
  startLiveOwner,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { holdSutOwnershipAcrossRestart } from './sut-owner.mjs'
import { runSutStart } from './sut-start.mjs'
import { healthyOnce, makeStartSpy } from './sut-start-fixtures.mjs'
import {
  RETIREMENT_ADMISSION_GATE_DIR_NAME,
  RETIREMENT_MARKER_NAME,
  acquireRetirementAdmission,
  releaseRetirementAdmission,
  retirementAdmissionGatePath,
  writeRetirementMarker,
} from './worktree-retirement-admission.mjs'

test('held retirement admission gate refuses isolated SUT start before provision or spawn', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  mkdirSync(retirementAdmissionGatePath(checkout.root))
  const spawn = makeStartSpy()
  const mysql = recordingMysql()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawn, {
      schemaExistsFn: () => false,
      mysqlExecFn: mysql.mysqlExecFn,
    }),
    new RegExp(RETIREMENT_ADMISSION_GATE_DIR_NAME)
  )
  assert.equal(mysql.calls.length, 0)
  assert.equal(spawn.calls.length, 0)
})

test('seeded retirement marker refuses isolated SUT start before provisioning', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root, {
    id: completeIsolatedConfig.id,
    e2e: {
      backendPort: 19081,
      vitePort: 15174,
      lbListenPort: 15173,
    },
  })
  writeRetirementMarker(checkout.root)
  const spawn = makeStartSpy()
  const mysql = recordingMysql()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawn, {
      schemaExistsFn: () => false,
      mysqlExecFn: mysql.mysqlExecFn,
    }),
    new RegExp(RETIREMENT_MARKER_NAME)
  )
  assert.equal(mysql.calls.length, 0)
  assert.equal(spawn.calls.length, 0)
  assert.equal(existsSync(`${checkout.root}/.sut.local.lock`), false)
})

test('successful isolated start releases the retirement admission gate', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const spawn = makeStartSpy()
  const code = await runConfiguredStart(checkout.root, spawn)
  assert.equal(code, 0)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
})

test('retained-owner restart start does not take the retirement admission gate', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  await startLiveOwner(checkout.root, t)
  await holdSutOwnershipAcrossRestart(checkout.root)
  acquireRetirementAdmission(checkout.root)
  t.after(() => releaseRetirementAdmission(checkout.root))
  const spawn = makeStartSpy()
  const code = await runSutStart({
    checkoutRoot: checkout.root,
    retainOwnership: true,
    spawnFn: spawn.spawnFn,
    logFile: `${checkout.root}/sut.log`,
    pidFile: `${checkout.root}/sut.pid`,
    timeoutMs: 5_000,
    pollMs: 50,
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: healthyOnce,
    databaseExistsFn: () => true,
  })
  assert.equal(code, 0)
  assert.equal(spawn.calls.length, 1)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), true)
})
