import {
  mkdirSync,
  readFileSync,
  renameSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import {
  assertValidWorktreeId,
  worktreeLocalConfigPath,
} from './worktree-identity.mjs'

export const E2E_PORT_FIELDS = ['backendPort', 'vitePort', 'lbListenPort']
export const E2E_PORT_CONFIG_FIELDS = E2E_PORT_FIELDS.map(
  (field) => `e2e.${field}`
)

const CLAIM_LOCK_DIR_NAME = 'lock'
const CLAIMS_FILE_NAME = 'claims.json'
const CLAIM_LOCK_WAIT_MS = 5000
const CLAIM_LOCK_POLL_MS = 20

function defaultE2ePortClaimRoot() {
  return path.join(tmpdir(), 'doughnut-worktree-e2e-port-claims')
}

export function collectMissingE2ePorts(e2e) {
  if (!e2e || typeof e2e !== 'object') {
    return [...E2E_PORT_CONFIG_FIELDS]
  }
  const missing = []
  for (const field of E2E_PORT_FIELDS) {
    if (!Number.isInteger(e2e[field]) || e2e[field] <= 0) {
      missing.push(`e2e.${field}`)
    }
  }
  return missing
}

function recordedE2eApplicationPorts(e2e) {
  return Object.fromEntries(E2E_PORT_FIELDS.map((field) => [field, e2e[field]]))
}

export function isolatedE2ePortsRequiredError(checkoutRoot, missing) {
  return new Error(
    `Isolated worktree SUT needs identity and application ports in ${worktreeLocalConfigPath(
      checkoutRoot
    )} (id, ${E2E_PORT_CONFIG_FIELDS.join(', ')}). ` +
      `Missing or invalid: ${missing.join(', ')}. See docs/worktree-browser-tests.md.`
  )
}

function sleepSync(ms) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms)
}

function claimLockDir(claimRoot) {
  return path.join(claimRoot, CLAIM_LOCK_DIR_NAME)
}

function claimsFilePath(claimRoot) {
  return path.join(claimRoot, CLAIMS_FILE_NAME)
}

function readLockOwnerPid(lockDir) {
  try {
    const raw = readFileSync(path.join(lockDir, 'owner.pid'), 'utf8').trim()
    if (raw === '') return null
    if (!/^[0-9]+$/.test(raw)) {
      throw new Error('E2E port claim lock record is invalid.')
    }
    return Number(raw)
  } catch (error) {
    if (error.code === 'ENOENT') return null
    throw error
  }
}

function isLivePid(pid) {
  try {
    process.kill(pid, 0)
    return true
  } catch {
    return false
  }
}

function tryReclaimStaleClaimLock(lockDir) {
  const pid = readLockOwnerPid(lockDir)
  if (pid === null) return false
  if (isLivePid(pid)) return false
  try {
    mkdirSync(path.join(lockDir, `reclaimed.${pid}`))
  } catch {
    return false
  }
  rmSync(lockDir, { recursive: true, force: true })
  return true
}

function acquireE2ePortClaimLock(claimRoot) {
  mkdirSync(claimRoot, { recursive: true })
  const lockDir = claimLockDir(claimRoot)
  const deadline = Date.now() + CLAIM_LOCK_WAIT_MS
  while (Date.now() < deadline) {
    try {
      mkdirSync(lockDir)
      writeFileSync(path.join(lockDir, 'owner.pid'), String(process.pid))
      return
    } catch (error) {
      if (error.code !== 'EEXIST') throw error
      if (!tryReclaimStaleClaimLock(lockDir)) {
        sleepSync(CLAIM_LOCK_POLL_MS)
      }
    }
  }
  throw new Error(
    'Timed out waiting for the machine-local E2E port claim lock.'
  )
}

function releaseE2ePortClaimLock(claimRoot) {
  rmSync(claimLockDir(claimRoot), { recursive: true, force: true })
}

export function withE2ePortClaimLock(claimRoot, fn) {
  const root = claimRoot ?? defaultE2ePortClaimRoot()
  acquireE2ePortClaimLock(root)
  try {
    return fn()
  } finally {
    releaseE2ePortClaimLock(root)
  }
}

export function readPublishedE2ePortClaims(claimRoot) {
  try {
    const raw = readFileSync(claimsFilePath(claimRoot), 'utf8')
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('E2E port claim registry is invalid.')
    }
    return parsed
  } catch (error) {
    if (error.code === 'ENOENT') return {}
    throw error
  }
}

function writePublishedE2ePortClaims(claimRoot, claims) {
  const filePath = claimsFilePath(claimRoot)
  const tempPath = `${filePath}.${process.pid}.tmp`
  writeFileSync(tempPath, JSON.stringify(claims))
  renameSync(tempPath, filePath)
}

function readCheckoutConfig(checkoutRoot) {
  const configPath = worktreeLocalConfigPath(checkoutRoot)
  const config = JSON.parse(readFileSync(configPath, 'utf8'))
  if (typeof config.id !== 'string') {
    throw new Error('Worktree configuration must contain a string "id".')
  }
  assertValidWorktreeId(config.id)
  return config
}

function recordCheckoutPorts(checkoutRoot, config, ports) {
  const next = {
    ...config,
    e2e: {
      ...(config.e2e && typeof config.e2e === 'object' ? config.e2e : {}),
      ...ports,
    },
  }
  writeFileSync(worktreeLocalConfigPath(checkoutRoot), JSON.stringify(next))
  return next
}

function publishRecordedPorts(claimRoot, checkoutRoot) {
  const config = readCheckoutConfig(checkoutRoot)
  const missing = collectMissingE2ePorts(config.e2e)
  if (missing.length > 0) {
    throw isolatedE2ePortsRequiredError(checkoutRoot, missing)
  }
  const ports = recordedE2eApplicationPorts(config.e2e)
  const claims = readPublishedE2ePortClaims(claimRoot)
  claims[config.id] = ports
  writePublishedE2ePortClaims(claimRoot, claims)
  recordCheckoutPorts(checkoutRoot, config, ports)
  return ports
}

export function ensureIsolatedE2ePorts(checkoutRoot, { claimRoot } = {}) {
  const root = claimRoot ?? defaultE2ePortClaimRoot()
  return withE2ePortClaimLock(root, () =>
    publishRecordedPorts(root, checkoutRoot)
  )
}
