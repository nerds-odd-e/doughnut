/**
 * Process-level MySQL admin adapter for worktree database retirement.
 * Production defaults to local root@127.0.0.1:3309. Tests bind a disposable
 * instance here — not via product target overrides.
 */
import { execFileSync } from 'node:child_process'

const DEFAULT_MYSQL_ADMIN = Object.freeze({
  user: 'root',
  host: '127.0.0.1',
  port: 3309,
})

/** @type {{ user?: string, host?: string, port?: number, mysqlExecFn?: Function } | null} */
let processMysqlTestAdapter = null

export function bindWorktreeRetirementMysqlTestAdapter(adapter) {
  processMysqlTestAdapter = adapter
  return () => {
    if (processMysqlTestAdapter === adapter) {
      processMysqlTestAdapter = null
    }
  }
}

export function worktreeRetirementMysqlAdminArgs() {
  const adapter = processMysqlTestAdapter
  return [
    '-u',
    adapter?.user ?? DEFAULT_MYSQL_ADMIN.user,
    '-h',
    adapter?.host ?? DEFAULT_MYSQL_ADMIN.host,
    '-P',
    String(adapter?.port ?? DEFAULT_MYSQL_ADMIN.port),
  ]
}

export function worktreeRetirementMysqlExec(file, args, options) {
  const execFn = processMysqlTestAdapter?.mysqlExecFn ?? execFileSync
  return execFn(file, args, options)
}

/** Run a non-interactive MySQL admin statement against the retirement adapter. */
export function runRetirementMysqlAdmin(sql, { mysqlExecFn } = {}) {
  const execFn =
    mysqlExecFn ??
    ((file, args, options) => worktreeRetirementMysqlExec(file, args, options))
  return execFn(
    'mysql',
    [...worktreeRetirementMysqlAdminArgs(), '-N', '-B', '-e', sql],
    {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    }
  )
}

/** True when information_schema lists the schema name. */
export function retirementSchemaExists(database, { mysqlExecFn } = {}) {
  const escaped = String(database).replace(/'/g, "''")
  const stdout = runRetirementMysqlAdmin(
    `SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${escaped}'`,
    { mysqlExecFn }
  )
  return String(stdout).trim() === database
}

/**
 * Drop an allocated disposable schema when present.
 * Missing schemas count as already absent (IF EXISTS); never provisions.
 */
export function dropRetirementDatabase(database, { mysqlExecFn } = {}) {
  if (!/^[A-Za-z0-9_]+$/.test(database)) {
    throw new Error(
      `Refusing worktree database retirement DROP: unsafe database name ${JSON.stringify(
        database
      )}.`
    )
  }
  const existed = retirementSchemaExists(database, { mysqlExecFn })
  runRetirementMysqlAdmin(`DROP DATABASE IF EXISTS \`${database}\``, {
    mysqlExecFn,
  })
  return existed ? 'dropped' : 'already absent'
}
