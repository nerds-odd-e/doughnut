import { randomBytes } from 'node:crypto'
import { mkdir, readFile, rm, unlink, writeFile } from 'node:fs/promises'
import path from 'node:path'
import {
  ownerRecordPath,
  readOwnerRecord,
  sutOwnerLockDir,
  verifyLiveSutOwner,
} from './sut-owner-control.mjs'

export {
  SUT_OWNER_LOCK_DIR_NAME,
  acquireSutRunnerLease,
  beginSutOwnerShutdown,
  releaseSutRunnerLease,
  releaseSutRunnerLeaseSync,
  startSutOwnerControl,
  startSutOwnerControlFromEnv,
  sutOwnerLockDir,
  verifyLiveSutOwner,
} from './sut-owner-control.mjs'

async function writeStartingPid(checkoutRoot) {
  await writeFile(startingPidPath(checkoutRoot), String(process.pid))
}

export async function holdSutOwnershipAcrossRestart(checkoutRoot) {
  await writeStartingPid(checkoutRoot)
}

export async function readHeldSutOwner(checkoutRoot) {
  const owner = await readOwnerRecord(checkoutRoot)
  if (owner?.token && owner?.controlPath) return owner
  throw new Error('Isolated restart could not read the held SUT owner record.')
}

function duplicateStartError() {
  return new Error(
    'SUT is already running in this checkout. Refusing a duplicate start; the existing owner was left running.'
  )
}

export async function assertNoLiveSutOwner(checkoutRoot) {
  if ((await verifyLiveSutOwner(checkoutRoot)).ok) {
    throw duplicateStartError()
  }
}

function startingPidPath(checkoutRoot) {
  return path.join(sutOwnerLockDir(checkoutRoot), 'starting.pid')
}

async function isLiveStartingPid(checkoutRoot) {
  try {
    const pid = Number(await readFile(startingPidPath(checkoutRoot), 'utf8'))
    if (!Number.isInteger(pid) || pid <= 0) return false
    process.kill(pid, 0)
    return true
  } catch {
    return false
  }
}

async function tryAcquireLockDir(lockDir) {
  try {
    await mkdir(lockDir)
    return true
  } catch (error) {
    if (error.code === 'EEXIST') return false
    throw error
  }
}

async function reclaimDeadOwner(lockDir) {
  const reclaimDir = path.join(lockDir, `reclaim.${process.pid}`)
  try {
    await mkdir(reclaimDir)
  } catch {
    throw duplicateStartError()
  }
  try {
    await unlink(path.join(lockDir, 'owner.sock'))
  } catch {
    // absent
  }
}

export async function claimSutOwnership(checkoutRoot) {
  const lockDir = sutOwnerLockDir(checkoutRoot)
  const acquired = await tryAcquireLockDir(lockDir)
  if (!acquired) {
    if (
      (await verifyLiveSutOwner(checkoutRoot)).ok ||
      (await isLiveStartingPid(checkoutRoot))
    ) {
      throw duplicateStartError()
    }
    await reclaimDeadOwner(lockDir)
  }
  const token = randomBytes(16).toString('hex')
  const controlPath = path.join(lockDir, 'owner.sock')
  await writeStartingPid(checkoutRoot)
  await writeFile(
    ownerRecordPath(checkoutRoot),
    JSON.stringify({ token, controlPath })
  )
  return { token, controlPath, lockDir }
}

export async function releaseSutOwnership(checkoutRoot) {
  await rm(sutOwnerLockDir(checkoutRoot), { recursive: true, force: true })
}
