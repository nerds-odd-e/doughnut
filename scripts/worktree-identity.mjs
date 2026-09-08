import { randomUUID } from 'node:crypto'
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'

const WORKTREE_LOCAL_CONFIG_NAME = '.worktree.local.json'
const WORKTREE_IDENTITY_LOCK_DIR_NAME = '.worktree.identity.lock'
export const WORKTREE_ID_PATTERN = /^wt_[a-z0-9_]{1,32}$/

const INIT_LOCK_WAIT_MS = 5000
const INIT_LOCK_POLL_MS = 20

export function worktreeLocalConfigPath(checkoutRoot) {
  return path.join(checkoutRoot, WORKTREE_LOCAL_CONFIG_NAME)
}

function generateWorktreeId() {
  return `wt_${randomUUID().replace(/-/g, '')}`
}

export function assertValidWorktreeId(worktreeId) {
  if (typeof worktreeId !== 'string' || !WORKTREE_ID_PATTERN.test(worktreeId)) {
    throw new Error(`Worktree id must match wt_[a-z0-9_]{1,32}: ${worktreeId}`)
  }
}

function identityInitLockDir(checkoutRoot) {
  return path.join(checkoutRoot, WORKTREE_IDENTITY_LOCK_DIR_NAME)
}

function sleepSync(ms) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms)
}

function readInitLockOwnerPid(lockDir) {
  try {
    const raw = readFileSync(path.join(lockDir, 'owner.pid'), 'utf8').trim()
    if (raw === '') return null
    if (!/^[0-9]+$/.test(raw)) {
      throw new Error(
        'Worktree identity initialization lock record is invalid.'
      )
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

function tryReclaimStaleInitLock(lockDir) {
  const pid = readInitLockOwnerPid(lockDir)
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

function acquireIdentityInitLock(checkoutRoot) {
  const lockDir = identityInitLockDir(checkoutRoot)
  const deadline = Date.now() + INIT_LOCK_WAIT_MS
  while (Date.now() < deadline) {
    try {
      mkdirSync(lockDir)
      writeFileSync(path.join(lockDir, 'owner.pid'), String(process.pid))
      return
    } catch (error) {
      if (error.code !== 'EEXIST') throw error
      if (!tryReclaimStaleInitLock(lockDir)) {
        sleepSync(INIT_LOCK_POLL_MS)
      }
    }
  }
  throw new Error(
    'Timed out waiting for worktree identity initialization lock.'
  )
}

function releaseIdentityInitLock(checkoutRoot) {
  rmSync(identityInitLockDir(checkoutRoot), {
    recursive: true,
    force: true,
  })
}

export function withIdentityInitLock(checkoutRoot, fn) {
  acquireIdentityInitLock(checkoutRoot)
  try {
    return fn()
  } finally {
    releaseIdentityInitLock(checkoutRoot)
  }
}

function readWorktreeIdentityUnlocked(checkoutRoot) {
  const config = JSON.parse(
    readFileSync(worktreeLocalConfigPath(checkoutRoot), 'utf8')
  )
  if (typeof config.id !== 'string') {
    throw new Error('Worktree configuration must contain a string "id".')
  }
  assertValidWorktreeId(config.id)
  return config
}

function writeExclusiveIdentity(checkoutRoot, worktreeId) {
  writeFileSync(
    worktreeLocalConfigPath(checkoutRoot),
    JSON.stringify({ id: worktreeId }),
    { flag: 'wx' }
  )
}

export function readWorktreeIdentity(checkoutRoot) {
  return withIdentityInitLock(checkoutRoot, () => {
    const config = readWorktreeIdentityUnlocked(checkoutRoot)
    return { id: config.id }
  })
}

export function publishWorktreeIdentity(checkoutRoot, worktreeId) {
  assertValidWorktreeId(worktreeId)
  return withIdentityInitLock(checkoutRoot, () => {
    writeExclusiveIdentity(checkoutRoot, worktreeId)
    return { id: worktreeId }
  })
}

export function initializeWorktreeIdentity(checkoutRoot) {
  return withIdentityInitLock(checkoutRoot, () => {
    try {
      const config = readWorktreeIdentityUnlocked(checkoutRoot)
      return { id: config.id }
    } catch (error) {
      if (error.code !== 'ENOENT') throw error
    }
    const id = generateWorktreeId()
    writeExclusiveIdentity(checkoutRoot, id)
    return { id }
  })
}

function runCli(argv) {
  const [, , command, checkoutRoot, worktreeId] = argv
  if (command === 'generate') {
    process.stdout.write(generateWorktreeId())
    return
  }
  if (!checkoutRoot) {
    throw new Error('Worktree identity command requires a checkout root.')
  }
  if (command === 'read') {
    process.stdout.write(readWorktreeIdentity(checkoutRoot).id)
    return
  }
  if (command === 'publish') {
    publishWorktreeIdentity(checkoutRoot, worktreeId)
    return
  }
  throw new Error(`Unknown worktree identity command: ${command}`)
}

const CLI_COMMANDS = new Set(['generate', 'read', 'publish'])

function isCliEntry() {
  const entry = process.argv[1]
  return Boolean(
    entry &&
      path.basename(entry) === 'worktree-identity.mjs' &&
      CLI_COMMANDS.has(process.argv[2])
  )
}

if (isCliEntry()) {
  try {
    runCli(process.argv)
  } catch (error) {
    process.stderr.write(`${error instanceof Error ? error.stack : error}\n`)
    process.exit(1)
  }
}
