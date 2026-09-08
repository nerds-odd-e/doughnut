import { randomBytes } from 'node:crypto'
import { unlinkSync } from 'node:fs'
import http from 'node:http'

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
    const body = {
      ok: true,
      pid: process.pid,
      runnerLeaseHeld: Boolean(state.runnerLeaseToken),
    }
    const applicationGroupId = state.getApplicationGroupId?.()
    if (Number.isInteger(applicationGroupId) && applicationGroupId > 0) {
      body.applicationGroupId = applicationGroupId
    }
    sendJson(res, 200, body)
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
    if (state.shuttingDown) {
      sendJson(res, 409, {
        error: 'SUT owner is already shutting down.',
      })
      return
    }
    if (state.runnerLeaseToken) {
      sendJson(res, 409, {
        error:
          'A Cypress runner is using this checkout. Refusing restart; owned processes were not signalled.',
      })
      return
    }
    state.shuttingDown = true
    sendJson(res, 200, { ok: true })
    if (state.onShutdown) {
      res.once('finish', () => {
        Promise.resolve(state.onShutdown()).catch((error) => {
          process.stderr.write(
            `SUT owner shutdown failed: ${
              error instanceof Error ? error.message : String(error)
            }\n`
          )
          process.exit(1)
        })
      })
    }
    return
  }
  res.writeHead(404)
  res.end()
}

export function startSutOwnerControl({
  token,
  controlPath,
  onShutdown,
  getApplicationGroupId,
} = {}) {
  try {
    unlinkSync(controlPath)
  } catch {
    // absent leftover socket
  }
  const state = {
    token,
    runnerLeaseToken: null,
    shuttingDown: false,
    onShutdown,
    getApplicationGroupId,
  }
  const server = http.createServer((req, res) => {
    handleOwnerControlRequest(req, res, state)
  })
  return new Promise((resolve, reject) => {
    server.once('error', reject)
    server.listen(controlPath, () => resolve(server))
  })
}

export function startSutOwnerControlFromEnv(
  env = process.env,
  { onShutdown, getApplicationGroupId } = {}
) {
  const token = env.SUT_OWNER_TOKEN
  const controlPath = env.SUT_OWNER_CONTROL_PATH
  if (!(token && controlPath)) return Promise.resolve(null)
  return startSutOwnerControl({
    token,
    controlPath,
    onShutdown,
    getApplicationGroupId,
  })
}
