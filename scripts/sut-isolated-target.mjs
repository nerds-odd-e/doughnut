import { execFileSync } from 'node:child_process'
import {
  loadCompleteIsolatedE2eAllocation,
  worktreeIsolationApplies,
} from './browser-worktree-isolation.mjs'
import {
  resolveSutRuntimeTarget,
  sutRuntimeTargetProcessEnv,
} from './sut-runtime-target.mjs'

export { isolatedBrowserOrigin } from './sut-runtime-target.mjs'

const E2E_JDBC_PARAMS =
  'connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'

function e2eJdbcUrl(database) {
  return `jdbc:mysql://127.0.0.1:3309/${database}?${E2E_JDBC_PARAMS}`
}

export function isolatedRuntimeTargetFromConfig(config) {
  return {
    backendPort: config.e2e.backendPort,
    vitePort: config.e2e.vitePort,
    lbListenPort: config.e2e.lbListenPort,
    databaseUrl: e2eJdbcUrl(config.e2e.database),
    database: config.e2e.database,
  }
}

function resolveIsolatedSutRuntimeTarget(checkoutRoot) {
  return isolatedRuntimeTargetFromConfig(
    loadCompleteIsolatedE2eAllocation(checkoutRoot)
  )
}

export function resolveSutCheckoutTarget({ checkoutRoot, runtimeTarget } = {}) {
  if (worktreeIsolationApplies(checkoutRoot)) {
    return {
      isolated: true,
      target: resolveIsolatedSutRuntimeTarget(checkoutRoot),
    }
  }
  return {
    isolated: false,
    target: resolveSutRuntimeTarget({ runtimeTarget }),
  }
}

export function refuseConflictingSutOverrides(env, target) {
  const assigned = sutRuntimeTargetProcessEnv(target)
  const exact = [
    ['SERVER_PORT', assigned.SERVER_PORT],
    ['LOCAL_LB_BACKEND', assigned.LOCAL_LB_BACKEND],
    ['LOCAL_LB_VITE_UPSTREAM', assigned.LOCAL_LB_VITE_UPSTREAM],
    ['LOCAL_LB_LISTEN_PORT', assigned.LOCAL_LB_LISTEN_PORT],
    ['FRONTEND_DEV_PORT', assigned.FRONTEND_DEV_PORT],
    ['FRONTEND_BACKEND_ORIGIN', assigned.FRONTEND_BACKEND_ORIGIN],
  ]
  for (const [name, expected] of exact) {
    const value = env[name]
    if (value && value !== expected) {
      throw new Error(
        `Conflicting ${name}=${value} does not match the configured isolated SUT target (${expected}).`
      )
    }
  }
  for (const name of [
    'INPUT_DB_URL',
    'SPRING_DATASOURCE_URL',
    'DB_URL',
    'SPRING_FLYWAY_URL',
  ]) {
    const value = env[name]
    if (value && value !== target.databaseUrl) {
      throw new Error(
        `Conflicting ${name} does not match the configured isolated E2E database ${target.database}.`
      )
    }
  }
  if (!env.SUT_RUNTIME_TARGET) return
  const given = JSON.parse(env.SUT_RUNTIME_TARGET)
  if (
    given.backendPort !== target.backendPort ||
    given.vitePort !== target.vitePort ||
    given.lbListenPort !== target.lbListenPort ||
    (given.databaseUrl && given.databaseUrl !== target.databaseUrl) ||
    given.mountebankPort != null
  ) {
    throw new Error(
      'Conflicting SUT_RUNTIME_TARGET does not match the configured isolated SUT target.'
    )
  }
}

function e2eDatabaseExists(database) {
  const stdout = execFileSync(
    'mysql',
    [
      '-h127.0.0.1',
      '-P3309',
      '-udoughnut',
      '-pdoughnut',
      '-N',
      '-e',
      `SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${database}'`,
    ],
    { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }
  )
  return stdout.trim() === database
}

export function assertE2eDatabaseExists(database, { existsFn } = {}) {
  const exists = existsFn ? existsFn(database) : e2eDatabaseExists(database)
  if (!exists) {
    throw new Error(
      `Recorded E2E database ${database} does not exist. Refusing to start; will not create, adopt, or rebuild it.`
    )
  }
}

export async function assertAllocatedPortsFree(target, isPortOccupiedFn) {
  const ports = [
    ['backend', target.backendPort],
    ['frontend vite', target.vitePort],
    ['local LB', target.lbListenPort],
  ]
  const occupied = []
  for (const [service, port] of ports) {
    if (await isPortOccupiedFn(port)) {
      occupied.push(`${service} ${port}`)
    }
  }
  if (occupied.length === 0) return
  throw new Error(
    `Isolated SUT ports are already occupied (${occupied.join(', ')}). ` +
      'Refusing to start; the listener was not terminated.'
  )
}
