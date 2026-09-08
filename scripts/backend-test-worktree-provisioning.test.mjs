import assert from 'node:assert/strict'
import { existsSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { test } from 'node:test'
import {
  jdbcUrl,
  makeCheckout,
  readGradleInvocation,
  readMysqlInvocation,
  readMysqlInvocations,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  assertRefusedBeforeGradle,
  outputOf,
  runLauncher,
  runLauncherAsync,
} from './backend-test-worktree-launcher-fixtures.mjs'
import { lockPaths } from './backend-test-worktree-lock-fixtures.mjs'

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

  // The failure reports the generated target/stage it was diagnosing, and
  // the native mysql failure (nonzero exit) still terminates the script
  // rather than being caught and replaced with a generic message.
  const match = outputOf(result).match(
    /Provisioning database administration.*(doughnut_wt_[a-z0-9]{32}_test).*worktree id (wt_[a-z0-9]{32})/
  )
  assert.ok(match, outputOf(result))
  const [, reportedDatabase, reportedId] = match
  assert.equal(reportedDatabase, `doughnut_${reportedId}_test`)
})

test('an overlapping command refuses while first-use provisioning is in flight, leaving one usable environment', async (t) => {
  const checkout = makeCheckout(t)
  const owner = runLauncherAsync(checkout)
  await owner.waitForGradleReached()

  const configPath = `${checkout.root}/.worktree.local.json`
  const provisionedConfig = JSON.parse(readFileSync(configPath, 'utf8'))
  assert.match(provisionedConfig.id, /^wt_[a-z0-9]{32}$/)
  const mysqlAfterProvisioning = readMysqlInvocation(checkout)

  const second = runLauncher(checkout)
  assert.notEqual(second.status, 0)
  assert.match(outputOf(second), /already running/i)
  assert.doesNotMatch(outputOf(second), /Allocated new worktree environment/)
  assert.doesNotMatch(outputOf(second), /GRADLE_STDOUT|GRADLE_REACHED/)

  // The overlapping command never reached provisioning or config: no second
  // mysql call, and the owner's config is untouched.
  assert.deepEqual(readMysqlInvocation(checkout), mysqlAfterProvisioning)
  assert.deepEqual(
    JSON.parse(readFileSync(configPath, 'utf8')),
    provisionedConfig
  )

  owner.release()
  const ownerResult = await owner.waitForExit()
  assert.equal(ownerResult.status, 0, ownerResult.stderr)

  const database = `doughnut_${provisionedConfig.id}_test`
  assert.equal(readGradleInvocation(checkout).url, jdbcUrl(database))

  // The environment remains usable after the owner exits: the config is
  // complete, parseable, and still names the database the owner actually
  // migrated and tested against.
  assert.deepEqual(
    JSON.parse(readFileSync(configPath, 'utf8')),
    provisionedConfig
  )
})

test('an identity published by another actor during provisioning is preserved, and the launcher refuses without adopting either database', async (t) => {
  const checkout = makeCheckout(t)
  const owner = runLauncherAsync(checkout, { env: { MYSQL_HOLD: '1' } })
  await owner.waitForMysqlReached()

  // While the launcher's own mysql call is held (after it succeeded, before
  // the launcher writes its config), another actor publishes a config first.
  const configPath = `${checkout.root}/.worktree.local.json`
  const winningConfig = JSON.stringify({ id: 'wt_winner' })
  writeFileSync(configPath, winningConfig)

  owner.releaseMysql()
  const result = await owner.waitForExit()

  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /EEXIST/)
  // The winning config is untouched, byte-for-byte: not overwritten with the
  // launcher's own generated id, and not adopted/rewritten either.
  assert.equal(readFileSync(configPath, 'utf8'), winningConfig)
})

test('existing configuration skips CREATE when the unit database already exists', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout)
  assert.equal(result.status, 0, outputOf(result))
  assert.doesNotMatch(outputOf(result), /Allocated new worktree environment/)
  const sql = readMysqlInvocation(checkout).args.at(-1)
  assert.match(sql, /information_schema\.SCHEMATA/)
  assert.equal(sql.includes('CREATE DATABASE'), false)
  assert.equal(sql.includes('GRANT'), false)
  assert.equal(
    JSON.parse(readFileSync(`${checkout.root}/.worktree.local.json`, 'utf8'))
      .id,
    'wt_a7c2'
  )
})

test('a later invocation reuses the config a first-use run produced and skips CREATE', (t) => {
  const checkout = makeCheckout(t)
  const first = runLauncher(checkout)
  assert.equal(first.status, 0, outputOf(first))

  const configPath = `${checkout.root}/.worktree.local.json`
  const provisionedConfig = readFileSync(configPath, 'utf8')
  const database = `doughnut_${JSON.parse(provisionedConfig).id}_test`

  // Clear the leftover checkout lock so a later command can acquire a fresh
  // lock and reuse the established identity without going through stale
  // reclaim.
  rmSync(lockPaths(checkout).dir, { recursive: true, force: true })

  const second = runLauncher(checkout)
  assert.equal(second.status, 0, outputOf(second))
  assert.doesNotMatch(outputOf(second), /Allocated new worktree environment/)
  assert.match(outputOf(second), new RegExp(`Selected database: ${database}`))

  const mysqlCalls = readMysqlInvocations(checkout)
  assert.equal(mysqlCalls.length, 2)
  assert.match(
    mysqlCalls[0].args.at(-1),
    new RegExp(`CREATE DATABASE ${database} `)
  )
  assert.match(mysqlCalls[1].args.at(-1), /information_schema\.SCHEMATA/)
  assert.equal(mysqlCalls[1].args.at(-1).includes('CREATE DATABASE'), false)

  // The config is unchanged, field-for-field.
  assert.equal(readFileSync(configPath, 'utf8'), provisionedConfig)

  assert.equal(readGradleInvocation(checkout).url, jdbcUrl(database))
})
