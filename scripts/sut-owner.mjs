import { randomBytes } from 'node:crypto'
import { mkdir, readFile, unlink, writeFile } from 'node:fs/promises'
import { unlinkSync } from 'node:fs'
import http from 'node:http'
import path from 'node:path'

export const SUT_OWNER_LOCK_DIR_NAME = '.sut.local.lock'

export function sutOwnerLockDir(checkoutRoot) {
  return path.join(checkoutRoot, SUT_OWNER_LOCK_DIR_NAME)
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

function ownerRecordPath(checkoutRoot) {
  return path.join(sutOwnerLockDir(checkoutRoot), 'owner.json')
}

function startingPidPath(checkoutRoot) {
  return path.join(sutOwnerLockDir(checkoutRoot), 'starting.pid')
}

export function startSutOwnerControl({ token, controlPath }) {
  try {
    unlinkSync(controlPath)
  } catch {
    // absent leftover socket
  }
  const server = http.createServer((req, res) => {
    if (
      req.url !== '/owner' ||
      req.headers.authorization !== `Bearer ${token}`
    ) {
      res.writeHead(403)
      res.end()
      return
    }
    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ ok: true, pid: process.pid }))
  })
  return new Promise((resolve, reject) => {
    server.once('error', reject)
    server.listen(controlPath, () => resolve(server))
  })
}

export function startSutOwnerControlFromEnv(env = process.env) {
  const token = env.SUT_OWNER_TOKEN
  const controlPath = env.SUT_OWNER_CONTROL_PATH
  if (!(token && controlPath)) return Promise.resolve(null)
  return startSutOwnerControl({ token, controlPath })
}

function requestLiveOwner(controlPath, token, timeoutMs = 500) {
  return new Promise((resolve) => {
    const req = http.request(
      {
        socketPath: controlPath,
        path: '/owner',
        headers: { Authorization: `Bearer ${token}` },
        timeout: timeoutMs,
      },
      (res) => {
        let body = ''
        res.on('data', (chunk) => {
          body += chunk
        })
        res.on('end', () => {
          if (res.statusCode !== 200) {
            resolve({ ok: false })
            return
          }
          try {
            resolve({ ok: true, ...JSON.parse(body) })
          } catch {
            resolve({ ok: false })
          }
        })
      }
    )
    req.on('error', () => resolve({ ok: false }))
    req.on('timeout', () => {
      req.destroy()
      resolve({ ok: false })
    })
    req.end()
  })
}

export async function verifyLiveSutOwner(checkoutRoot) {
  let owner
  try {
    owner = JSON.parse(await readFile(ownerRecordPath(checkoutRoot), 'utf8'))
  } catch {
    return { ok: false }
  }
  if (!(owner?.token && owner?.controlPath)) return { ok: false }
  return requestLiveOwner(owner.controlPath, owner.token)
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
  await writeFile(startingPidPath(checkoutRoot), String(process.pid))
  await writeFile(
    ownerRecordPath(checkoutRoot),
    JSON.stringify({ token, controlPath })
  )
  return { token, controlPath, lockDir }
}
