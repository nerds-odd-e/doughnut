import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { test } from 'node:test'
import {
  assertRefusedBeforeGradle,
  jdbcUrl,
  makeCheckout,
  outputOf,
  readGradleInvocation,
  readMysqlInvocation,
  runLauncher,
} from './backend-test-worktree-test-fixtures.mjs'

const allocatedIdPattern =
  /Allocated new worktree environment: (wt_[a-z0-9]{32})/

test('missing configuration provisions one new database, then writes config, then runs gradle against it', (t) => {
  const checkout = makeCheckout(t)
  const result = runLauncher(checkout)
  assert.equal(result.error, undefined, result.stderr)
  assert.equal(result.status, 0, outputOf(result))

  const match = outputOf(result).match(allocatedIdPattern)
  assert.ok(match, outputOf(result))
  const [, allocatedId] = match
  assert.match(allocatedId, /^wt_[a-z0-9_]{1,32}$/)
  const database = `doughnut_${allocatedId}_test`

  const mysqlInvocation = readMysqlInvocation(checkout)
  assert.deepEqual(mysqlInvocation.args.slice(0, 6), [
    '-u',
    'root',
    '-h',
    '127.0.0.1',
    '-P',
    '3309',
  ])
  assert.equal(mysqlInvocation.args[6], '-e')
  const sql = mysqlInvocation.args[7]
  assert.match(
    sql,
    new RegExp(
      `CREATE DATABASE ${database} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
    )
  )
  assert.match(
    sql,
    new RegExp(
      `GRANT ALL PRIVILEGES ON ${database}\\.\\* TO 'doughnut'@'localhost';`
    )
  )
  assert.match(
    sql,
    new RegExp(
      `GRANT ALL PRIVILEGES ON ${database}\\.\\* TO 'doughnut'@'127\\.0\\.0\\.1';`
    )
  )
  assert.match(sql, /FLUSH PRIVILEGES;/)

  const config = JSON.parse(
    readFileSync(`${checkout.root}/.worktree.local.json`, 'utf8')
  )
  assert.deepEqual(config, { id: allocatedId })

  const gradleInvocation = readGradleInvocation(checkout)
  assert.equal(gradleInvocation.url, jdbcUrl(database))
  assert.match(outputOf(result), new RegExp(`Selected database: ${database}`))
  // Allocation is announced before database selection.
  assert.ok(
    outputOf(result).indexOf('Allocated new worktree environment:') <
      outputOf(result).indexOf('Selected database:')
  )
})

test('failed database administration leaves no config and never reaches gradle', (t) => {
  const checkout = makeCheckout(t)
  const result = runLauncher(checkout, { env: { FAKE_MYSQL_EXIT: '1' } })
  assertRefusedBeforeGradle(checkout, result)
  assert.equal(existsSync(`${checkout.root}/.worktree.local.json`), false)
  assert.doesNotMatch(outputOf(result), /Selected database/)
})

test('existing configuration skips provisioning entirely', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout)
  assert.equal(result.status, 0, outputOf(result))
  assert.doesNotMatch(outputOf(result), /Allocated new worktree environment/)
  assert.equal(existsSync(checkout.mysqlInvocation), false)
  assert.equal(
    JSON.parse(readFileSync(`${checkout.root}/.worktree.local.json`, 'utf8'))
      .id,
    'wt_a7c2'
  )
})
