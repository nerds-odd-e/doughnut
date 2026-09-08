import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { makeLinkedWorktreeCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  jdbcUrl,
  readGradleInvocation,
  readMysqlInvocation,
  readMysqlInvocations,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  outputOf,
  runLauncher,
} from './backend-test-worktree-launcher-fixtures.mjs'
import { e2eDatabaseNameForIdentity } from './sut-e2e-database.mjs'
import {
  readIsolatedConfig,
  runConfiguredStart,
} from './sut-isolated-fixtures.mjs'
import { makeStartSpy } from './sut-start-fixtures.mjs'

async function startLinkedSut(t) {
  const checkout = makeLinkedWorktreeCheckout(t)
  const code = await runConfiguredStart(checkout.root, makeStartSpy(), {
    schemaExistsFn: () => false,
    mysqlExecFn: () => '',
  })
  assert.equal(code, 0)
  return checkout
}

test('backend-test after SUT-first provisions the unit database and reuses the identity', async (t) => {
  const checkout = await startLinkedSut(t)
  const before = readIsolatedConfig(checkout.root)
  const rawBefore = readFileSync(
    `${checkout.root}/.worktree.local.json`,
    'utf8'
  )
  const unitDatabase = `doughnut_${before.id}_test`

  const result = runLauncher(checkout, { env: { FAKE_SCHEMA_MISSING: '1' } })
  assert.equal(result.status, 0, outputOf(result))
  assert.doesNotMatch(outputOf(result), /Allocated new worktree environment/)
  assert.match(
    outputOf(result),
    new RegExp(`Selected database: ${unitDatabase}`)
  )

  const createSql = readMysqlInvocations(checkout)
    .map((invocation) => invocation.args.at(-1))
    .find((sql) => sql?.includes('CREATE DATABASE'))
  assert.match(createSql, new RegExp(`CREATE DATABASE ${unitDatabase} `))
  assert.equal(createSql.includes('IF NOT EXISTS'), false)
  assert.equal(createSql.includes('doughnut_e2e_test'), false)
  assert.equal(createSql.includes('doughnut_test'), false)
  assert.notEqual(before.e2e.database, unitDatabase)
  assert.equal(before.e2e.database, e2eDatabaseNameForIdentity(before.id))
  assert.equal(
    readFileSync(`${checkout.root}/.worktree.local.json`, 'utf8'),
    rawBefore
  )
  assert.equal(readGradleInvocation(checkout).url, jdbcUrl(unitDatabase))
})

test('backend-test after SUT-first reuses an existing unit database without rewriting E2E', async (t) => {
  const checkout = await startLinkedSut(t)
  const before = readIsolatedConfig(checkout.root)
  const rawBefore = readFileSync(
    `${checkout.root}/.worktree.local.json`,
    'utf8'
  )
  const unitDatabase = `doughnut_${before.id}_test`

  const result = runLauncher(checkout)
  assert.equal(result.status, 0, outputOf(result))
  assert.match(
    outputOf(result),
    new RegExp(`Selected database: ${unitDatabase}`)
  )
  const sql = readMysqlInvocation(checkout).args.at(-1)
  assert.match(sql, /information_schema\.SCHEMATA/)
  assert.equal(sql.includes('CREATE DATABASE'), false)
  assert.equal(sql.includes('GRANT'), false)
  assert.equal(
    readFileSync(`${checkout.root}/.worktree.local.json`, 'utf8'),
    rawBefore
  )
  assert.equal(readGradleInvocation(checkout).url, jdbcUrl(unitDatabase))
})
