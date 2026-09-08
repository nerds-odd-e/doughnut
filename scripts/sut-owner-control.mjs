import { randomBytes } from 'node:crypto'
import { readFile } from 'node:fs/promises'
import { unlinkSync } from 'node:fs'
import http from 'node:http'
import path from 'node:path'

export const SUT_OWNER_LOCK_DIR_NAME = '.sut.local.lock'

export function sutOwnerLockDir(checkoutRoot) {
  return path.join(checkoutRoot, SUT_OWNER_LOCK_DIR_NAME)
}

export function ownerRecordPath(checkoutRoot) {
  return path.join(sutOwnerLockDir(checkoutRoot), 'owner.json')
}

function sendJson(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json' })
  res.end(JSON.stringify(body))
}

function handleOwnerControlRequest(req, res, state) {
  if (req.headers.authorization !== `Bearer ${state.token}`) {
    res.writeHead(403)
    res.end()
    return
  }
  const urlPath = (req.url ?? '').split('?')[0]
  if (req.method === 'GET' && urlPath === '/owner') {
    sendJson(res, 200, { ok: true, pid: process.pid })
    return
  }
  if (req.method === 'POST' && urlPath === '/runner-lease') {
    if (state.shuttingDown) {
      sendJson(res, 409, {
        error: 'SUT owner is shutting down; refusing a new Cypress runner.',
      })
      return
    }
    if (state.runnerLeaseToken) {
      sendJson(res, 409, {
        error:
          'A Cypress runner is already using this checkout. Refusing a duplicate runner.',
      })
      return
    }
    state.runnerLeaseToken = randomBytes(16).toString('hex')
    sendJson(res, 200, { ok: true, token: state.runnerLeaseToken })
    return
  }
  if (req.method === 'DELETE' && urlPath === '/runner-lease') {
    const presented = req.headers['x-sut-runner-lease']
    if (!state.runnerLeaseToken) {
      sendJson(res, 200, { ok: true })
      return
    }
    if (presented !== state.runnerLeaseToken) {
      sendJson(res, 409, {
        error: 'Cypress runner lease token does not match the held lease.',
      })
      return
    }
    state.runnerLeaseToken = null
    sendJson(res, 200, { ok: true })
    return
  }
  if (req.method === 'POST' && urlPath === '/shutdown') {
    state.shuttingDown = true
    sendJson(res, 200, {
      ok: true,
      runnerLeaseHeld: Boolean(state.runnerLeaseToken),
    })
    return
  }
  res.writeHead(404)
  res.end()
}

export function startSutOwnerControl({ token, controlPath }) {
  try {
    unlinkSync(controlPath)
  } catch {
    // absent leftover socket
  }
  const state = { token, runnerLeaseToken: null, shuttingDown: false }
  const server = http.createServer((req, res) => {
    handleOwnerControlRequest(req, res, state)
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

async function readOwnerRecord(checkoutRoot) {
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
