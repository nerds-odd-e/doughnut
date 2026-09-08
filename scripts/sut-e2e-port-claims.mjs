import {
  mkdirSync,
  readFileSync,
  renameSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'

const CLAIM_LOCK_DIR_NAME = 'lock'
const CLAIMS_FILE_NAME = 'claims.json'
const CLAIM_LOCK_WAIT_MS = 5000
const CLAIM_LOCK_POLL_MS = 20

export function defaultE2ePortClaimRoot() {
  return path.join(tmpdir(), 'doughnut-worktree-e2e-port-claims')
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

export async function withE2ePortClaimLock(claimRoot, fn) {
  const root = claimRoot ?? defaultE2ePortClaimRoot()
  acquireE2ePortClaimLock(root)
  try {
    return await fn()
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

export function writePublishedE2ePortClaims(claimRoot, claims) {
  const filePath = claimsFilePath(claimRoot)
  const tempPath = `${filePath}.${process.pid}.tmp`
  writeFileSync(tempPath, JSON.stringify(claims))
  renameSync(tempPath, filePath)
}
