#!/usr/bin/env node
/**
 * Local fake LB for Cypress / dev. Behavior and ports: docs/gcp/prod_env.md (section **Local E2E / dev**).
 *
 * Env: LOCAL_LB_STATIC_ROOT, LOCAL_LB_BACKEND, LOCAL_LB_VITE_UPSTREAM, LOCAL_LB_LISTEN_PORT (defaults in code).
 * LOCAL_LB_ROUTING_JSON: optional path to doughnut-routing.json (default: repo infra/gcp/path-routing/doughnut-routing.json).
 */
import { createReadStream, existsSync, statSync } from 'node:fs'
import http from 'node:http'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { loadDoughnutRouting } from '../infra/gcp/path-routing/doughnutRouting.mjs'
import { pathGoesToBackend } from '../infra/gcp/path-routing/pathGoesToBackend.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..')
const STATIC_ROOT = path.resolve(
  process.env.LOCAL_LB_STATIC_ROOT ?? path.join(repoRoot, 'frontend/dist')
)
const BACKEND = new URL(process.env.LOCAL_LB_BACKEND ?? 'http://127.0.0.1:9081')
const VITE_UPSTREAM = process.env.LOCAL_LB_VITE_UPSTREAM
  ? new URL(process.env.LOCAL_LB_VITE_UPSTREAM)
  : null
const PORT = Number(process.env.LOCAL_LB_LISTEN_PORT ?? 5173)
const DOUGHNUT_ROUTING_PATH = process.env.LOCAL_LB_ROUTING_JSON
  ? path.resolve(process.env.LOCAL_LB_ROUTING_JSON)
  : path.join(repoRoot, 'infra/gcp/path-routing/doughnut-routing.json')
const DOUGHNUT_ROUTING = loadDoughnutRouting(DOUGHNUT_ROUTING_PATH)
const BACKEND_PATH_HINTS = DOUGHNUT_ROUTING.backendPathHints
const LOCAL_SPA_SHELL_PATHS = new Set(
  DOUGHNUT_ROUTING.localProxy?.spaShellInsteadOfBackendExactPaths ?? []
)

/** Default: same as prod GCS object; from `pnpm cli:bundle`. */
const CLI_DEFAULT_INSTALL_BUNDLE = path.join(
  repoRoot,
  'cli/dist/doughnut-cli.bundle.mjs'
)
/** Cypress `@bundleCliE2eInstall` builds here so install scenarios do not overwrite the default bundle. */
const CLI_CYPRESS_ALT_INSTALL_BUNDLE = path.join(
  repoRoot,
  'cli/dist/e2e-install-doughnut-cli.bundle.mjs'
)

function cliInstallBundlePath() {
  if (existsSync(CLI_CYPRESS_ALT_INSTALL_BUNDLE))
    return CLI_CYPRESS_ALT_INSTALL_BUNDLE
  return CLI_DEFAULT_INSTALL_BUNDLE
}

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.ico': 'image/x-icon',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.ttf': 'font/ttf',
  '.map': 'application/json; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8',
}

function shouldProxyPath(urlPath) {
  if (LOCAL_SPA_SHELL_PATHS.has(urlPath)) return false
  return pathGoesToBackend(urlPath, BACKEND_PATH_HINTS)
}

/** Backend vs Vite for normal app traffic (not `/__lb__/ready`). */
function upstreamForPath(urlPath) {
  if (shouldProxyPath(urlPath)) return BACKEND
  if (VITE_UPSTREAM) return VITE_UPSTREAM
  return null
}

function proxyLogLabel(target) {
  return target === BACKEND ? 'backend' : 'vite'
}

function safeStaticPath(urlPath) {
  const pathname = urlPath.split('?')[0]
  const rel = path
    .normalize(decodeURIComponent(pathname))
    .replace(/^(\.\.(\/|\\|$))+/, '')
  const trimmed = rel.replace(/^\//, '')
  const full = path.resolve(STATIC_ROOT, trimmed)
  if (!full.startsWith(path.resolve(STATIC_ROOT))) return null
  return full
}

function sendStatic(filePath, method, res) {
  let st
  try {
    st = statSync(filePath)
  } catch {
    return false
  }
  if (st.isDirectory()) {
    const idx = path.join(filePath, 'index.html')
    if (existsSync(idx)) return sendStatic(idx, method, res)
    return false
  }
  const ext = path.extname(filePath).toLowerCase()
  const type = MIME[ext] ?? 'application/octet-stream'
  res.statusCode = 200
  res.setHeader('Content-Type', type)
  res.setHeader('Content-Length', st.size)
  if (method === 'HEAD') {
    res.end()
    return true
  }
  createReadStream(filePath).pipe(res)
  return true
}

function serveSpaIndex(method, res) {
  const indexPath = path.join(STATIC_ROOT, 'index.html')
  return sendStatic(indexPath, method, res)
}

/** Prod serves this URL from GCS; locally we stream the bundled CLI (not in the jar). */
function tryServeCliInstallPath(urlPath, method, res) {
  const pathname = (urlPath.split('?')[0] || '/').replace(/\/+$/, '') || '/'
  if (pathname !== '/doughnut-cli-latest/doughnut') return false
  if (method !== 'GET' && method !== 'HEAD') return false
  const bundlePath = cliInstallBundlePath()
  if (!existsSync(bundlePath)) {
    res.statusCode = 404
    res.end('Not Found')
    return true
  }
  return sendStatic(bundlePath, method, res)
}

function targetPort(target) {
  if (target.port) return target.port
  return target.protocol === 'https:' ? '443' : '80'
}

function backendPortNumber() {
  return Number(targetPort(BACKEND))
}

/** Used by CI wait-on: proxy is up and can reach Spring /api/healthcheck (server-side; bypasses HTTP_PROXY on wait-on). */
function probeBackendHealth(done) {
  const opts = {
    hostname: BACKEND.hostname,
    port: backendPortNumber(),
    path: '/api/healthcheck',
    method: 'GET',
    timeout: 10_000,
  }
  const pReq = http.request(opts, (pRes) => {
    const ok =
      pRes.statusCode !== undefined &&
      pRes.statusCode >= 200 &&
      pRes.statusCode < 300
    pRes.resume()
    pRes.on('end', () => done(ok))
  })
  pReq.on('error', () => done(false))
  pReq.on('timeout', () => {
    pReq.destroy()
    done(false)
  })
  pReq.end()
}

function handleLbReady(req, res) {
  const method = req.method ?? 'GET'
  if (method !== 'GET' && method !== 'HEAD') {
    res.statusCode = 405
    res.end('Method Not Allowed')
    return
  }
  probeBackendHealth((ok) => {
    res.statusCode = ok ? 200 : 503
    res.setHeader('Content-Type', 'text/plain; charset=utf-8')
    if (method === 'HEAD') {
      res.end()
      return
    }
    res.end(ok ? 'ready' : 'not ready')
  })
}

function proxyHttp(clientReq, clientRes, target, logLabel) {
  const headers = { ...clientReq.headers }
  const incomingHost = clientReq.headers.host ?? `127.0.0.1:${PORT}`
  const tp = targetPort(target)
  headers.host = `${target.hostname}:${tp}`
  headers['x-forwarded-host'] = incomingHost
  headers['x-forwarded-proto'] = 'http'
  delete headers.connection

  const opts = {
    protocol: target.protocol,
    hostname: target.hostname,
    port: tp,
    path: clientReq.url,
    method: clientReq.method,
    headers,
  }

  const pReq = http.request(opts, (pRes) => {
    clientRes.writeHead(pRes.statusCode ?? 502, pRes.headers)
    pRes.pipe(clientRes)
  })
  pReq.on('error', (err) => {
    console.error(`[local-lb] ${logLabel} error:`, err.message)
    if (!clientRes.headersSent) {
      clientRes.statusCode = 502
      clientRes.end('Bad Gateway')
    }
  })
  clientReq.pipe(pReq)
}

function rawUpgradeHead(res) {
  const code = res.statusCode ?? 101
  const msg = res.statusMessage || 'Switching Protocols'
  let raw = `HTTP/1.1 ${code} ${msg}\r\n`
  for (const [key, value] of Object.entries(res.headers)) {
    if (value === undefined) continue
    if (Array.isArray(value)) {
      for (const v of value) raw += `${key}: ${v}\r\n`
    } else {
      raw += `${key}: ${value}\r\n`
    }
  }
  raw += '\r\n'
  return raw
}

function proxyUpgrade(clientReq, clientSocket, head, target, logLabel) {
  const tp = targetPort(target)
  const headers = { ...clientReq.headers }
  headers.host = `${target.hostname}:${tp}`

  const opts = {
    hostname: target.hostname,
    port: tp,
    path: clientReq.url,
    method: clientReq.method,
    headers,
  }

  const pReq = http.request(opts)
  pReq.on('upgrade', (pRes, pSocket, pHead) => {
    clientSocket.write(rawUpgradeHead(pRes))
    const endSocket = () => {
      clientSocket.destroy()
      pSocket.destroy()
    }
    pSocket.on('error', endSocket)
    clientSocket.on('error', endSocket)
    if (pHead?.length) pSocket.unshift(pHead)
    pSocket.pipe(clientSocket)
    clientSocket.pipe(pSocket)
  })
  pReq.on('error', (err) => {
    console.error(`[local-lb] ${logLabel} upgrade error:`, err.message)
    clientSocket.destroy()
  })
  pReq.end(head)
}

if (!(VITE_UPSTREAM || existsSync(STATIC_ROOT))) {
  console.error(`[local-lb] static root missing: ${STATIC_ROOT}`)
  process.exit(1)
}

const server = http.createServer((req, res) => {
  const urlPath = (req.url ?? '/').split('?')[0] || '/'
  if (urlPath === '/__lb__/ready') {
    handleLbReady(req, res)
    return
  }
  const method = req.method ?? 'GET'
  if (tryServeCliInstallPath(urlPath, method, res)) return
  const up = upstreamForPath(urlPath)
  if (up) {
    proxyHttp(req, res, up, proxyLogLabel(up))
    return
  }
  if (method !== 'GET' && method !== 'HEAD') {
    res.statusCode = 405
    res.end('Method Not Allowed')
    return
  }
  const filePath = safeStaticPath(req.url ?? '/')
  if (filePath && existsSync(filePath)) {
    if (sendStatic(filePath, method, res)) return
  }
  if (method === 'GET' || method === 'HEAD') {
    if (serveSpaIndex(method, res)) return
  }
  res.statusCode = 404
  res.end('Not Found')
})

server.on('upgrade', (req, socket, head) => {
  const urlPath = (req.url ?? '/').split('?')[0] || '/'
  const up = upstreamForPath(urlPath)
  if (up) {
    proxyUpgrade(req, socket, head, up, proxyLogLabel(up))
    return
  }
  socket.destroy()
})

server.listen(PORT, '0.0.0.0', () => {
  const staticOrVite = VITE_UPSTREAM
    ? `vite=${VITE_UPSTREAM.origin}`
    : `static=${STATIC_ROOT}`
  console.log(
    `[local-lb] http://127.0.0.1:${PORT} ${staticOrVite} -> ${BACKEND.origin}`
  )
})
