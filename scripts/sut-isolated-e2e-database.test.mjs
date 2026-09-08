import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  e2eDatabaseNameForIdentity,
  e2eDatabaseProvisioningSql,
} from './sut-e2e-database.mjs'
import {
  identityAndPortsConfig,
  readIsolatedConfig,
  runConfiguredStart,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { isolatedRuntimeTargetFromConfig } from './sut-isolated-target.mjs'
import { makeStartSpy } from './sut-start-fixtures.mjs'

const expectedDatabase = e2eDatabaseNameForIdentity(identityAndPortsConfig.id)

function recordingMysql() {
  const calls = []
  return {
    calls,
    mysqlExecFn(_file, args) {
      calls.push(args)
      return ''
    },
  }
}

function firstUseOpts(extra = {}) {
  const mysql = extra.mysql ?? recordingMysql()
  return {
    mysql,
    opts: {
      schemaExistsFn: extra.schemaExistsFn ?? (() => false),
      mysqlExecFn: extra.mysqlExecFn ?? mysql.mysqlExecFn,
      log: extra.log,
    },
  }
}

test('first-use isolated start provisions identity-derived E2E database then starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root, {
    ...identityAndPortsConfig,
    extra: 'keep-me',
  })
  const spawn = makeStartSpy()
  const logs = []
  const { mysql, opts } = firstUseOpts({ log: (line) => logs.push(line) })

  const code = await runConfiguredStart(checkout.root, spawn, opts)
  assert.equal(code, 0)
  assert.equal(mysql.calls.length, 1)
  const sql = mysql.calls[0].at(-1)
  assert.equal(sql, e2eDatabaseProvisioningSql(expectedDatabase))
  assert.equal(sql.includes('IF NOT EXISTS'), false)
  assert.equal(sql.includes('DROP'), false)
  assert.equal(sql.includes('doughnut_test'), false)
  assert.equal(sql.includes('doughnut_e2e_test'), false)
  assert.deepEqual(mysql.calls[0].slice(0, 6), [
    '-u',
    'root',
    '-h',
    '127.0.0.1',
    '-P',
    '3309',
  ])
  assert.ok(
    logs.some(
      (line) =>
        line ===
        `Provisioning E2E database ${expectedDatabase} (worktree id wt_a7c2)...`
    )
  )

  const config = readIsolatedConfig(checkout.root)
  assert.equal(config.id, 'wt_a7c2')
  assert.equal(config.extra, 'keep-me')
  assert.equal(config.e2e.database, expectedDatabase)
  assert.equal(config.e2e.backendPort, 19081)
  assert.equal(config.e2e.vitePort, 15174)
  assert.equal(config.e2e.lbListenPort, 15173)
  assert.equal(existsSync(`${checkout.root}/.worktree.local.lock`), false)
  assert.equal(spawn.calls.length, 1)
  const target = isolatedRuntimeTargetFromConfig(config)
  assert.equal(spawn.calls[0][2].env.INPUT_DB_URL, target.databaseUrl)
})

test('first-use collision does not grant, record, or start', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root, identityAndPortsConfig)
  const spawn = makeStartSpy()
  const mysql = recordingMysql()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawn, {
      schemaExistsFn: () => true,
      mysqlExecFn: mysql.mysqlExecFn,
    }),
    /already exists|will not adopt/
  )
  assert.equal(mysql.calls.length, 0)
  assert.deepEqual(readIsolatedConfig(checkout.root), identityAndPortsConfig)
  assert.equal(spawn.calls.length, 0)
})

test('failed E2E database preparation leaves no recorded database and does not start', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root, identityAndPortsConfig)
  const spawn = makeStartSpy()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawn, {
      schemaExistsFn: () => false,
      mysqlExecFn() {
        throw new Error('mysql administration failed')
      },
    }),
    /mysql administration failed/
  )
  assert.deepEqual(readIsolatedConfig(checkout.root), identityAndPortsConfig)
  assert.equal(spawn.calls.length, 0)
})

test('identity-only start still refuses before provisioning', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const spawn = makeStartSpy()
  const mysql = recordingMysql()
  await assert.rejects(
    runConfiguredStart(checkout.root, spawn, {
      schemaExistsFn: () => false,
      mysqlExecFn: mysql.mysqlExecFn,
    }),
    /Missing or invalid/
  )
  assert.equal(mysql.calls.length, 0)
  assert.equal(spawn.calls.length, 0)
  assert.deepEqual(readIsolatedConfig(checkout.root), { id: 'wt_a7c2' })
})

test('already recorded E2E database skips provisioning', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const spawn = makeStartSpy()
  const mysql = recordingMysql()
  const schemaCalls = []
  const code = await runConfiguredStart(checkout.root, spawn, {
    mysqlExecFn: mysql.mysqlExecFn,
    schemaExistsFn(database) {
      schemaCalls.push(database)
      return false
    },
  })
  assert.equal(code, 0)
  assert.equal(mysql.calls.length, 0)
  assert.deepEqual(schemaCalls, [])
  assert.equal(spawn.calls.length, 1)
})
