import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { readPresentWorktreeLocalConfig } from './browser-worktree-isolation.mjs'
import { E2E_PORT_FIELDS } from './sut-e2e-ports.mjs'
import {
  getListenerPids,
  isOwnedByApplicationTree,
} from './sut-listener-pids.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner-control.mjs'
import { inspectCheckoutBackendProcesses } from './worktree-retirement-checkout-processes.mjs'
import { runRetirementMysqlAdmin } from './worktree-retirement-mysql.mjs'

const WORKTREE_BACKEND_LOCK_DIR_NAME = '.worktree.local.lock'

function isLivePid(pid) {
  if (!Number.isInteger(pid) || pid <= 0) return false
  try {
    process.kill(pid, 0)
    return true
  } catch {
    return false
  }
}

function readPidFile(filePath) {
  try {
    const raw = readFileSync(filePath, 'utf8').trim()
    if (raw === '') return { kind: 'empty' }
    if (!/^[0-9]+$/.test(raw)) return { kind: 'invalid', raw }
    return { kind: 'pid', pid: Number(raw) }
  } catch (error) {
    if (error.code === 'ENOENT') return { kind: 'absent' }
    throw error
  }
}

function backendLockDir(checkoutRoot) {
  return path.join(checkoutRoot, WORKTREE_BACKEND_LOCK_DIR_NAME)
}

function startingPidPath(checkoutRoot) {
  return path.join(sutOwnerLockDir(checkoutRoot), 'starting.pid')
}

function inspectBackendWorktreeLock(checkoutRoot) {
  const lockDir = backendLockDir(checkoutRoot)
  if (!existsSync(lockDir)) return []
  const owner = readPidFile(path.join(lockDir, 'owner.pid'))
  if (owner.kind === 'pid' && isLivePid(owner.pid)) {
    return [
      `busy backend worktree owner (pid ${owner.pid} in ${WORKTREE_BACKEND_LOCK_DIR_NAME})`,
    ]
  }
  if (owner.kind === 'pid') {
    return [
      `stale or unverifiable backend worktree owner record (pid ${owner.pid} in ${WORKTREE_BACKEND_LOCK_DIR_NAME}; not reclaimed)`,
    ]
  }
  return [
    `stale or unverifiable backend worktree owner record (${WORKTREE_BACKEND_LOCK_DIR_NAME}; not reclaimed)`,
  ]
}

async function inspectSutOwnership(checkoutRoot) {
  const lockDir = sutOwnerLockDir(checkoutRoot)
  if (!existsSync(lockDir)) return { vetoes: [], live: null }

  const live = await verifyLiveSutOwner(checkoutRoot)
  if (live.ok) {
    const vetoes = ['busy live SUT owner']
    if (live.runnerLeaseHeld) {
      vetoes.push('busy Cypress runner lease')
    }
    return { vetoes, live }
  }

  const starting = readPidFile(startingPidPath(checkoutRoot))
  if (starting.kind === 'pid' && isLivePid(starting.pid)) {
    return {
      vetoes: [`busy SUT startup ownership (pid ${starting.pid})`],
      live: null,
    }
  }

  return {
    vetoes: [
      'stale or unverifiable SUT owner record (.sut.local.lock; not reclaimed)',
    ],
    live: null,
  }
}

function recordedApplicationPorts(checkoutRoot) {
  const config = readPresentWorktreeLocalConfig(checkoutRoot)
  const e2e = config?.e2e
  if (!e2e || typeof e2e !== 'object') return []
  const ports = []
  for (const field of E2E_PORT_FIELDS) {
    const port = e2e[field]
    if (Number.isInteger(port) && port >= 1 && port <= 65535) {
      ports.push({ field, port })
    }
  }
  return ports
}

async function inspectRecordedListeners(
  checkoutRoot,
  live,
  {
    getListenerPidsFn = getListenerPids,
    isOwnedByApplicationTreeFn = isOwnedByApplicationTree,
  } = {}
) {
  const vetoes = []
  for (const { field, port } of recordedApplicationPorts(checkoutRoot)) {
    let pids
    try {
      pids = await getListenerPidsFn(port)
    } catch (error) {
      const detail = error instanceof Error ? error.message : String(error)
      vetoes.push(
        `unable to inspect listeners on recorded e2e.${field} port ${port}: ${detail}`
      )
      continue
    }
    for (const pid of pids) {
      const applicationGroupId = live?.applicationGroupId
      const owned =
        Number.isInteger(applicationGroupId) &&
        applicationGroupId > 0 &&
        (await isOwnedByApplicationTreeFn(pid, applicationGroupId))
      if (owned) {
        vetoes.push(
          `busy owned listener PID ${pid} on recorded e2e.${field} port ${port}`
        )
      } else {
        vetoes.push(
          `foreign or unverified listener PID ${pid} on recorded e2e.${field} port ${port}`
        )
      }
    }
  }
  return vetoes
}

export function defaultListDatabaseSessions(databases, { mysqlExecFn } = {}) {
  const names = [...new Set(databases.filter(Boolean))]
  if (names.length === 0) return []
  const inList = names
    .map((name) => `'${String(name).replace(/'/g, "''")}'`)
    .join(',')
  const sql = `SELECT ID, USER, HOST, DB, COMMAND, TIME FROM information_schema.PROCESSLIST WHERE DB IN (${inList})`
  const stdout = runRetirementMysqlAdmin(sql, { mysqlExecFn })
  const sessions = []
  for (const line of String(stdout).split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed) continue
    const [id, user, host, db, command, time] = trimmed.split('\t')
    sessions.push({ id, user, host, db, command, time })
  }
  return sessions
}

async function inspectDatabaseSessions(
  targets,
  { listDatabaseSessionsFn = defaultListDatabaseSessions } = {}
) {
  const databases = [targets.unitDatabase, targets.e2eDatabase].filter(Boolean)
  let sessions
  try {
    sessions = await listDatabaseSessionsFn(databases)
  } catch (error) {
    const detail = error instanceof Error ? error.message : String(error)
    throw new Error(
      `Refusing worktree database retirement: unable to inspect database sessions for ${databases.join(
        ', '
      )}: ${detail}`
    )
  }
  if (!Array.isArray(sessions)) {
    throw new Error(
      'Refusing worktree database retirement: database session inspection returned ambiguous evidence.'
    )
  }
  return sessions.map((session) => {
    const db = session.db ?? session.DB ?? 'unknown'
    const id = session.id ?? session.ID ?? '?'
    const user = session.user ?? session.USER ?? '?'
    return `active database session id=${id} user=${user} db=${db}`
  })
}

/**
 * Bounded vetoes from recorded ownership, listeners, database sessions, and
 * surviving checkout backend JVMs. Never reclaims stale locks. Never authorizes
 * deletion.
 */
export async function collectRecordedRetirementVetoes(
  checkoutRoot,
  targets,
  deps = {}
) {
  const vetoes = []
  vetoes.push(...inspectBackendWorktreeLock(checkoutRoot))
  const { vetoes: sutVetoes, live } = await inspectSutOwnership(checkoutRoot)
  vetoes.push(...sutVetoes)
  vetoes.push(...(await inspectRecordedListeners(checkoutRoot, live, deps)))
  vetoes.push(...(await inspectDatabaseSessions(targets, deps)))
  vetoes.push(...(await inspectCheckoutBackendProcesses(checkoutRoot, deps)))
  return vetoes
}

export function formatRecordedEvidenceRefusal(vetoes) {
  return [
    'Refusing worktree database retirement: recorded evidence prevents cleanup.',
    ...vetoes.map((veto) => `- ${veto}`),
  ].join('\n')
}
