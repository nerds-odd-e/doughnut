#!/usr/bin/env node
/**
 * Single-origin E2E entrypoint (phase 6): serves built static from disk like prod GCS path,
 * proxies API/auth/attachments/install to Spring on E2E_PROXY_TARGET (default :9081).
 *
 * Env:
 *   E2E_STATIC_ROOT — default backend/src/main/resources/static (after pnpm bundle:all + frontend:build)
 *   E2E_PROXY_TARGET — default http://127.0.0.1:9081
 *   E2E_PROXY_LISTEN_PORT — default 5173 (same port as Vite for one mental model; CI uses proxy, not Vite).
 *     CI should wait on `http://127.0.0.1:5173/__e2e__/ready` — probes Spring at E2E_PROXY_TARGET using Node http (no corporate HTTP_PROXY), unlike wait-on hitting :9081 directly.
 */
import { createReadStream, existsSync, statSync } from 'node:fs'
import http from 'node:http'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..')
const STATIC_ROOT = path.resolve(
  process.env.E2E_STATIC_ROOT ??
    path.join(repoRoot, 'backend/src/main/resources/static')
)
const BACKEND = new URL(process.env.E2E_PROXY_TARGET ?? 'http://127.0.0.1:9081')
const PORT = Number(process.env.E2E_PROXY_LISTEN_PORT ?? 5173)

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
  if (urlPath === '/api' || urlPath.startsWith('/api/')) return true
  if (urlPath.startsWith('/attachments/')) return true
  if (urlPath.startsWith('/logout')) return true
  if (urlPath.startsWith('/users/')) return true
  if (urlPath === '/install') return true
  if (urlPath.startsWith('/oauth2/')) return true
  if (urlPath.startsWith('/login/oauth2/')) return true
  if (urlPath === '/login' || urlPath.startsWith('/login?')) return true
  if (urlPath === '/robots.txt') return true
  return false
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

function backendPortNumber() {
  if (BACKEND.port) return Number(BACKEND.port)
  return BACKEND.protocol === 'https:' ? 443 : 80
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

function handleE2eReady(req, res) {
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

function proxyToBackend(clientReq, clientRes) {
  const targetPath = clientReq.url ?? '/'
  const headers = { ...clientReq.headers }
  const incomingHost = clientReq.headers.host ?? `127.0.0.1:${PORT}`
  headers.host = `${BACKEND.hostname}:${BACKEND.port || (BACKEND.protocol === 'https:' ? 443 : 80)}`
  headers['x-forwarded-host'] = incomingHost
  headers['x-forwarded-proto'] = 'http'
  delete headers.connection

  const opts = {
    protocol: BACKEND.protocol,
    hostname: BACKEND.hostname,
    port: BACKEND.port || 80,
    path: targetPath,
    method: clientReq.method,
    headers,
  }

  const pReq = http.request(opts, (pRes) => {
    clientRes.writeHead(pRes.statusCode ?? 502, pRes.headers)
    pRes.pipe(clientRes)
  })
  pReq.on('error', (err) => {
    console.error('[e2e-prod-topology-proxy] backend error:', err.message)
    if (!clientRes.headersSent) {
      clientRes.statusCode = 502
      clientRes.end('Bad Gateway')
    }
  })
  clientReq.pipe(pReq)
}

if (!existsSync(STATIC_ROOT)) {
  console.error(`[e2e-prod-topology-proxy] static root missing: ${STATIC_ROOT}`)
  process.exit(1)
}

http
  .createServer((req, res) => {
    const urlPath = (req.url ?? '/').split('?')[0] || '/'
    if (urlPath === '/__e2e__/ready') {
      handleE2eReady(req, res)
      return
    }
    if (shouldProxyPath(urlPath)) {
      proxyToBackend(req, res)
      return
    }
    const method = req.method ?? 'GET'
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
  .listen(PORT, '0.0.0.0', () => {
    console.log(
      `[e2e-prod-topology-proxy] http://127.0.0.1:${PORT} static=${STATIC_ROOT} -> ${BACKEND.origin}`
    )
  })
