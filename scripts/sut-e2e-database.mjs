import { execFileSync } from 'node:child_process'
import { writeFileSync } from 'node:fs'
import {
  isRecordedE2eDatabase,
  readPresentWorktreeLocalConfig,
  refusePresentInvalidIsolatedE2eAllocation,
} from './browser-worktree-isolation.mjs'
import {
  assertValidWorktreeId,
  withIdentityInitLock,
  worktreeLocalConfigPath,
} from './worktree-identity.mjs'

const MYSQL_ADMIN_ARGS = ['-u', 'root', '-h', '127.0.0.1', '-P', '3309']

export function e2eDatabaseNameForIdentity(worktreeId) {
  assertValidWorktreeId(worktreeId)
  return `doughnut_e2e_${worktreeId}`
}

export function e2eDatabaseProvisioningSql(database) {
  if (!isRecordedE2eDatabase(database)) {
    throw new Error(`Invalid E2E database name: ${database}`)
  }
  return (
    `CREATE DATABASE ${database} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; ` +
    `GRANT ALL PRIVILEGES ON ${database}.* TO 'doughnut'@'localhost'; ` +
    `GRANT ALL PRIVILEGES ON ${database}.* TO 'doughnut'@'127.0.0.1'; ` +
    'FLUSH PRIVILEGES;'
  )
}

function runMysql(execFileFn, args) {
  return execFileFn('mysql', args, {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  })
}

function e2eAdminSchemaExists(database, { execFileFn = execFileSync } = {}) {
  const stdout = runMysql(execFileFn, [
    ...MYSQL_ADMIN_ARGS,
    '-N',
    '-e',
    `SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${database}'`,
  ])
  return String(stdout).trim() === database
}

function createAndGrantE2eDatabase(database, execFileFn) {
  runMysql(execFileFn, [
    ...MYSQL_ADMIN_ARGS,
    '-e',
    e2eDatabaseProvisioningSql(database),
  ])
}

function recordE2eDatabase(checkoutRoot, config, database) {
  const next = {
    ...config,
    e2e: {
      ...(config.e2e && typeof config.e2e === 'object' ? config.e2e : {}),
      database,
    },
  }
  writeFileSync(worktreeLocalConfigPath(checkoutRoot), JSON.stringify(next))
  return next
}

export function ensureIsolatedE2eDatabase(
  checkoutRoot,
  { mysqlExecFn, schemaExistsFn, log } = {}
) {
  const execFileFn = mysqlExecFn ?? execFileSync
  const writeLog = log ?? (() => undefined)
  return withIdentityInitLock(checkoutRoot, () => {
    const config = readPresentWorktreeLocalConfig(checkoutRoot)
    if (!config) {
      throw new Error(
        `Worktree configuration ${worktreeLocalConfigPath(checkoutRoot)} is missing.`
      )
    }
    assertValidWorktreeId(config.id)
    refusePresentInvalidIsolatedE2eAllocation(checkoutRoot, config, {
      start: true,
    })
    if (isRecordedE2eDatabase(config.e2e?.database)) {
      return config
    }
    const database = e2eDatabaseNameForIdentity(config.id)
    const exists = schemaExistsFn
      ? schemaExistsFn(database)
      : e2eAdminSchemaExists(database, { execFileFn })
    if (exists) {
      throw new Error(
        `E2E database ${database} already exists. Refusing to start; will not adopt, grant, or record it.`
      )
    }
    writeLog(
      `Provisioning E2E database ${database} (worktree id ${config.id})...`
    )
    createAndGrantE2eDatabase(database, execFileFn)
    return recordE2eDatabase(checkoutRoot, config, database)
  })
}
