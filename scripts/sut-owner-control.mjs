import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import http from 'node:http'
import path from 'node:path'

export {
  startSutOwnerControl,
  startSutOwnerControlFromEnv,
} from './sut-owner-control-server.mjs'

export const SUT_OWNER_LOCK_DIR_NAME = '.sut.local.lock'
export const SUT_OWNER_SOCKET_NAME = 'owner.sock'

export function sutOwnerLockDir(checkoutRoot) {
  return path.join(checkoutRoot, SUT_OWNER_LOCK_DIR_NAME)
}

export function ownerRecordPath(checkoutRoot) {
  return path.join(sutOwnerLockDir(checkoutRoot), 'owner.json')
}

function requestOwnerControl(
  controlPath,
  token,
  { method = 'GET', urlPath = '/owner', headers = {}, timeoutMs = 2_000 } = {}
) {
  return new Promise((resolve) => {
    const req = http.request(
      {
        socketPath: controlPath,
        path: urlPath,
        method,
        headers: {
          Authorization: `Bearer ${token}`,
          ...headers,
        },
        timeout: timeoutMs,
      },
      (res) => {
        let body = ''
        res.on('data', (chunk) => {
          body += chunk
        })
        res.on('end', () => {
          let parsed = {}
          if (body) {
            try {
              parsed = JSON.parse(body)
            } catch {
              parsed = { raw: body }
            }
          }
          resolve({
            ok: res.statusCode >= 200 && res.statusCode < 300,
            status: res.statusCode,
            ...parsed,
          })
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

export async function readOwnerRecord(checkoutRoot) {
  try {
    return JSON.parse(await readFile(ownerRecordPath(checkoutRoot), 'utf8'))
  } catch {
    return null
  }
}

export async function verifyLiveSutOwner(checkoutRoot) {
  const owner = await readOwnerRecord(checkoutRoot)
  if (!(owner?.token && owner?.controlPath)) return { ok: false }
  const result = await requestOwnerControl(owner.controlPath, owner.token)
  if (!result.ok) return { ok: false }
  return result
}

function noLiveOwnerError() {
  return new Error(
    'Isolated Cypress requires a verified live SUT owner in this checkout.'
  )
}

export async function acquireSutRunnerLease(checkoutRoot) {
  const live = await verifyLiveSutOwner(checkoutRoot)
  if (!live.ok) throw noLiveOwnerError()
  const owner = await readOwnerRecord(checkoutRoot)
  const result = await requestOwnerControl(owner.controlPath, owner.token, {
    method: 'POST',
    urlPath: '/runner-lease',
  })
  if (!(result.ok && result.token)) {
    throw new Error(
      result.error ??
        'Failed to acquire a Cypress runner lease for this checkout.'
    )
  }
  return result.token
}

export async function releaseSutRunnerLease(checkoutRoot, leaseToken) {
  const owner = await readOwnerRecord(checkoutRoot)
  if (!(owner?.token && owner?.controlPath && leaseToken)) return
  const result = await requestOwnerControl(owner.controlPath, owner.token, {
    method: 'DELETE',
    urlPath: '/runner-lease',
    headers: { 'X-Sut-Runner-Lease': leaseToken },
  })
  if (!result.ok && result.status) {
    throw new Error(
      result.error ?? 'Failed to release the Cypress runner lease.'
    )
  }
}

export function releaseSutRunnerLeaseSync(checkoutRoot, leaseToken) {
  let owner
  try {
    owner = JSON.parse(readFileSync(ownerRecordPath(checkoutRoot), 'utf8'))
  } catch {
    return
  }
  if (!(owner?.token && owner?.controlPath && leaseToken)) return
  const script = `
const http = require('http');
const req = http.request({
  socketPath: ${JSON.stringify(owner.controlPath)},
  path: '/runner-lease',
  method: 'DELETE',
  headers: {
    Authorization: ${JSON.stringify(`Bearer ${owner.token}`)},
    'X-Sut-Runner-Lease': ${JSON.stringify(leaseToken)},
  },
}, (res) => { res.resume(); res.on('end', () => process.exit(0)); });
req.on('error', () => process.exit(0));
req.setTimeout(2000, () => { req.destroy(); process.exit(0); });
req.end();
`
  try {
    execFileSync(process.execPath, ['-e', script], {
      timeout: 3000,
      stdio: 'ignore',
    })
  } catch {
    // owner already gone
  }
}

export async function beginSutOwnerShutdown(checkoutRoot) {
  const live = await verifyLiveSutOwner(checkoutRoot)
  if (!live.ok) throw noLiveOwnerError()
  const owner = await readOwnerRecord(checkoutRoot)
  const result = await requestOwnerControl(owner.controlPath, owner.token, {
    method: 'POST',
    urlPath: '/shutdown',
  })
  if (!result.ok) {
    throw new Error(result.error ?? 'Failed to begin SUT owner shutdown.')
  }
  return result
}
