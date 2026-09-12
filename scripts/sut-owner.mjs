import { randomBytes } from 'node:crypto'
import {
  mkdir,
  mkdtemp,
  readFile,
  rm,
  unlink,
  writeFile,
} from 'node:fs/promises'
import path from 'node:path'
import {
  ownerRecordPath,
  readOwnerRecord,
  SUT_OWNER_SOCKET_NAME,
  sutOwnerLockDir,
  verifyLiveSutOwner,
} from './sut-owner-control.mjs'

export {
  SUT_OWNER_LOCK_DIR_NAME,
  SUT_OWNER_SOCKET_NAME,
  acquireSutRunnerLease,
  beginSutOwnerShutdown,
  ownerRecordPath,
  releaseSutRunnerLease,
  releaseSutRunnerLeaseSync,
  startSutOwnerControl,
  startSutOwnerControlFromEnv,
  sutOwnerLockDir,
  verifyLiveSutOwner,
} from './sut-owner-control.mjs'

const OWNER_ENDPOINT_NAME_PREFIX = 'donut-sut-owner-'

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

function isOwnedPrivateEndpointDir(dir) {
  const normalized = path.normalize(dir)
  const name = path.basename(normalized)
  if (
    !name.startsWith(OWNER_ENDPOINT_NAME_PREFIX) ||
    name === OWNER_ENDPOINT_NAME_PREFIX
  ) {
    return false
  }
  const parent = path.dirname(normalized)
  return parent === '/tmp' || parent === '/private/tmp'
}

async function unlinkIfPresent(filePath) {
  try {
    await unlink(filePath)
  } catch (error) {
    if (error.code !== 'ENOENT') throw error
  }
}

async function removeOwnedEndpoint(controlPath, lockDir) {
  if (!controlPath) {
    await unlinkIfPresent(path.join(lockDir, SUT_OWNER_SOCKET_NAME))
    return
  }
  const endpointDir = path.dirname(controlPath)
  if (isOwnedPrivateEndpointDir(endpointDir)) {
    await rm(endpointDir, { recursive: true, force: true })
    return
  }
  await unlinkIfPresent(controlPath)
}

async function reclaimDeadOwner(checkoutRoot, lockDir) {
  const reclaimDir = path.join(lockDir, `reclaim.${process.pid}`)
  try {
    await mkdir(reclaimDir)
  } catch {
    throw duplicateStartError()
  }
  const owner = await readOwnerRecord(checkoutRoot)
  await removeOwnedEndpoint(owner?.controlPath, lockDir)
}

async function allocateOwnerEndpoint() {
  const endpointDir = await mkdtemp(`/tmp/${OWNER_ENDPOINT_NAME_PREFIX}`)
  return path.join(endpointDir, SUT_OWNER_SOCKET_NAME)
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
    await reclaimDeadOwner(checkoutRoot, lockDir)
  }
  const token = randomBytes(16).toString('hex')
  const controlPath = await allocateOwnerEndpoint()
  await writeStartingPid(checkoutRoot)
  await writeFile(
    ownerRecordPath(checkoutRoot),
    JSON.stringify({ token, controlPath })
  )
  return { token, controlPath, lockDir }
}

export async function releaseSutOwnership(checkoutRoot) {
  const lockDir = sutOwnerLockDir(checkoutRoot)
  const owner = await readOwnerRecord(checkoutRoot)
  await removeOwnedEndpoint(owner?.controlPath, lockDir)
  await rm(lockDir, { recursive: true, force: true })
}
