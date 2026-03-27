#!/usr/bin/env node
/**
 * Local SUT healthcheck for `pnpm sut`.
 * Run in this repo's shell wrapper: `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck`.
 * Topology reference: docs/gcp/prod_env.md (Local E2E / dev).
 */
import http from 'node:http'
import net from 'node:net'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export const DEFAULT_TCP_CHECKS = [
  { service: 'mountebank', host: '127.0.0.1', port: 2525 },
  { service: 'backend', host: '127.0.0.1', port: 9081 },
  { service: 'local LB', host: '127.0.0.1', port: 5173 },
  { service: 'frontend vite', host: '127.0.0.1', port: 5174 },
]

export const DEFAULT_READYNESS_URL = 'http://127.0.0.1:5173/__e2e__/ready'
const TCP_TIMEOUT_MS = 1_500
const HTTP_TIMEOUT_MS = 10_000

export function checkTcpPort({ host, port, timeoutMs = TCP_TIMEOUT_MS }) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host, port })
    let settled = false

    const finish = (result) => {
      if (settled) return
      settled = true
      socket.destroy()
      resolve(result)
    }

    socket.once('connect', () => finish({ ok: true }))
    socket.once('timeout', () =>
      finish({ ok: false, reason: `timeout after ${timeoutMs}ms` })
    )
    socket.once('error', (error) =>
      finish({ ok: false, reason: error.code ?? error.message })
    )
    socket.setTimeout(timeoutMs)
  })
}

export function checkHttpReady({ url, timeoutMs = HTTP_TIMEOUT_MS }) {
  return new Promise((resolve) => {
    const req = http.get(url, { timeout: timeoutMs }, (res) => {
      const status = res.statusCode ?? 0
      res.resume()
      res.on('end', () =>
        resolve({ ok: status >= 200 && status < 300, status })
      )
    })
    req.once('timeout', () => {
      req.destroy()
      resolve({ ok: false, reason: `timeout after ${timeoutMs}ms` })
    })
    req.once('error', (error) =>
      resolve({ ok: false, reason: error.code ?? error.message })
    )
  })
}

function formatTcpLine(result) {
  const endpoint = `${result.host}:${result.port}`
  if (result.ok) return `PASS TCP ${result.service} (${endpoint})`
  return `FAIL TCP ${result.service} (${endpoint}) - ${result.reason}`
}

function formatReadyLine(result) {
  if (result.skipped)
    return `SKIP HTTP readiness ${result.url} - ${result.reason}`
  if (result.ok) return `PASS HTTP readiness ${result.url} - ${result.status}`
  if (result.status !== undefined) {
    return `FAIL HTTP readiness ${result.url} - ${result.status}`
  }
  return `FAIL HTTP readiness ${result.url} - ${result.reason}`
}

export async function runSutHealthcheck({
  tcpChecks = DEFAULT_TCP_CHECKS,
  readinessUrl = DEFAULT_READYNESS_URL,
  log = console.log,
} = {}) {
  const tcpResults = []
  for (const check of tcpChecks) {
    const result = await checkTcpPort(check)
    const line = { ...check, ...result }
    tcpResults.push(line)
    log(formatTcpLine(line))
  }

  const lbUp = tcpResults.some(
    (result) => result.service === 'local LB' && result.ok
  )

  let readinessResult
  if (!lbUp) {
    readinessResult = {
      skipped: true,
      url: readinessUrl,
      reason: 'local LB not listening on 127.0.0.1:5173',
    }
  } else {
    readinessResult = {
      url: readinessUrl,
      ...(await checkHttpReady({ url: readinessUrl })),
    }
  }
  log(formatReadyLine(readinessResult))

  const hasTcpFailure = tcpResults.some((result) => !result.ok)
  const failed = hasTcpFailure || !readinessResult.ok
  if (failed) {
    log('SUT unhealthy or still starting.')
    log('If you just started `pnpm sut`, wait a few seconds and run again.')
    log('If services are down, start with `pnpm sut`.')
    return { ok: false, tcpResults, readinessResult, exitCode: 1 }
  }

  log('SUT healthcheck OK.')
  return { ok: true, tcpResults, readinessResult, exitCode: 0 }
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  const result = await runSutHealthcheck()
  process.exit(result.exitCode)
}
