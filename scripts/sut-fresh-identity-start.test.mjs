import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { existsSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
} from './backend-test-worktree-linked-fixtures.mjs'
import {
  jdbcUrl,
  readGradleInvocation,
  readMysqlInvocations,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  outputOf,
  runLauncher,
} from './backend-test-worktree-launcher-fixtures.mjs'
import {
  e2eDatabaseNameForIdentity,
  e2eDatabaseProvisioningSql,
} from './sut-e2e-database.mjs'
import {
  assertAllocatedIsolatedPorts,
  waitForClose,
} from './sut-e2e-port-test-helpers.mjs'
import {
  readIsolatedConfig,
  recordingMysql,
  runConfiguredStart,
  waitForFile,
} from './sut-isolated-fixtures.mjs'
import { makeStartSpy } from './sut-start-fixtures.mjs'
import { WORKTREE_ID_PATTERN } from './worktree-identity.mjs'

const sutStartHref = new URL('./sut-start.mjs', import.meta.url).href
const startFixturesHref = new URL('./sut-start-fixtures.mjs', import.meta.url)
  .href

test('linked checkout without identity initializes then starts without a backend-test lock', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const spawnSpy = makeStartSpy()
  const mysql = recordingMysql()
  const code = await runConfiguredStart(checkout.root, spawnSpy, {
    schemaExistsFn: () => false,
    mysqlExecFn: mysql.mysqlExecFn,
  })
  assert.equal(code, 0)
  const config = readIsolatedConfig(checkout.root)
  assert.match(config.id, WORKTREE_ID_PATTERN)
  const expectedDatabase = e2eDatabaseNameForIdentity(config.id)
  assert.equal(config.e2e.database, expectedDatabase)
  assertAllocatedIsolatedPorts(config.e2e)
  assert.equal(mysql.calls.length, 1)
  assert.equal(
    mysql.calls[0].at(-1),
    e2eDatabaseProvisioningSql(expectedDatabase)
  )
  assert.equal(existsSync(`${checkout.root}/.worktree.local.lock`), false)
  assert.equal(spawnSpy.calls.length, 1)
})

test('unconfigured primary start does not write a worktree identity', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const spawnSpy = makeStartSpy()
  const code = await runConfiguredStart(checkout.root, spawnSpy)
  assert.equal(code, 0)
  assert.equal(existsSync(`${checkout.root}/.worktree.local.json`), false)
  assert.equal(spawnSpy.calls.length, 1)
})

function runHeldSutStartProcess(checkout, healthRelease) {
  const child = spawn(
    process.execPath,
    [
      '--input-type=module',
      '-e',
      `import { existsSync } from 'node:fs'
import path from 'node:path'
import { runSutStart } from ${JSON.stringify(sutStartHref)}
import { healthyOnce, makeStartSpy } from ${JSON.stringify(startFixturesHref)}
const checkoutRoot = ${JSON.stringify(checkout.root)}
const healthRelease = ${JSON.stringify(healthRelease)}
const spawnSpy = makeStartSpy()
const code = await runSutStart({
  checkoutRoot,
  spawnFn: spawnSpy.spawnFn,
  logFile: path.join(checkoutRoot, 'sut.log'),
  pidFile: path.join(checkoutRoot, 'sut.pid'),
  timeoutMs: 15_000,
  pollMs: 50,
  log: () => undefined,
  errLog: () => undefined,
  schemaExistsFn: () => false,
  mysqlExecFn: () => '',
  databaseExistsFn: () => true,
  isPortOccupiedFn: async () => false,
  portClaimRoot: path.join(checkoutRoot, '.doughnut-e2e-port-claims'),
  healthcheckFn: async () => {
    if (existsSync(healthRelease)) return healthyOnce()
    return {
      ok: false,
      tcpResults: [],
      readinessResult: { ok: false },
      exitCode: 1,
    }
  },
})
process.exit(code)
`,
    ],
    { encoding: 'utf8' }
  )
  child.stdin.end()
  return child
}

test('overlapping SUT and backend first-use publish one identity with distinct purpose databases', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const healthRelease = path.join(checkout.root, 'health-release')
  const sut = runHeldSutStartProcess(checkout, healthRelease)
  const sutDone = waitForClose(sut)
  t.after(() => sut.kill())

  await waitForFile(`${checkout.root}/.worktree.local.json`)
  const backend = runLauncher(checkout, { env: { FAKE_SCHEMA_MISSING: '1' } })
  assert.equal(backend.status, 0, outputOf(backend))
  assert.doesNotMatch(outputOf(backend), /Allocated new worktree environment/)

  writeFileSync(healthRelease, 'go\n')
  const sutResult = await sutDone
  assert.equal(sutResult.status, 0, sutResult.stderr)

  const config = readIsolatedConfig(checkout.root)
  assert.match(config.id, WORKTREE_ID_PATTERN)
  const e2eDatabase = e2eDatabaseNameForIdentity(config.id)
  const unitDatabase = `doughnut_${config.id}_test`
  assert.equal(config.e2e.database, e2eDatabase)
  assert.notEqual(e2eDatabase, unitDatabase)
  assertAllocatedIsolatedPorts(config.e2e)
  const createSql = readMysqlInvocations(checkout)
    .map((invocation) => invocation.args.at(-1))
    .find((sql) => sql?.includes('CREATE DATABASE'))
  assert.match(createSql, new RegExp(`CREATE DATABASE ${unitDatabase} `))
  assert.equal(createSql.includes('IF NOT EXISTS'), false)
  assert.equal(createSql.includes(e2eDatabase), false)
  assert.equal(readGradleInvocation(checkout).url, jdbcUrl(unitDatabase))
  assert.equal(existsSync(`${checkout.root}/.worktree.identity.lock`), false)
})
