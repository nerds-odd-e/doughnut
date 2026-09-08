#!/usr/bin/env node
/**
 * Local SUT healthcheck for `pnpm sut`.
 * Run in this repo's shell wrapper: `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck`.
 * Topology reference: docs/gcp/prod_env.md (Local dev / Cypress).
 */
import http from 'node:http'
import net from 'node:net'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { refuseUnsupportedIsolatedBrowserCommand } from './browser-worktree-isolation.mjs'
import { resolveSutCheckoutTarget } from './sut-isolated-target.mjs'
import { verifyLiveSutOwner } from './sut-owner.mjs'
import { sutHealthEndpoints } from './sut-runtime-target.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

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

function logUnhealthyHints(log) {
  log('SUT unhealthy or still starting.')
  log('If you just started `pnpm sut`, wait a few seconds and run again.')
  log('If services are down, start with `pnpm sut`.')
}

export async function runSutHealthcheck({
  runtimeTarget,
  tcpChecks,
  readinessUrl,
  log = console.log,
  checkoutRoot = repoRoot,
} = {}) {
  refuseUnsupportedIsolatedBrowserCommand({
    checkoutRoot,
    command: 'pnpm sut:healthcheck',
  })
  const { isolated, target } = resolveSutCheckoutTarget({
    checkoutRoot,
    runtimeTarget,
  })
  if (isolated) {
    const live = await verifyLiveSutOwner(checkoutRoot)
    if (!live.ok) {
      log(
        "FAIL live SUT owner — control endpoint did not verify this checkout's owner"
      )
      logUnhealthyHints(log)
      return {
        ok: false,
        tcpResults: [],
        readinessResult: { ok: false, reason: 'no live owner' },
        exitCode: 1,
      }
    }
  }
  const endpoints = sutHealthEndpoints(target)
  const checks = tcpChecks ?? endpoints.tcpChecks
  const readyUrl = readinessUrl ?? endpoints.readinessUrl
  const tcpResults = []
  for (const check of checks) {
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
      url: readyUrl,
      reason: `local LB not listening on ${new URL(readyUrl).host}`,
    }
  } else {
    readinessResult = {
      url: readyUrl,
      ...(await checkHttpReady({ url: readyUrl })),
    }
  }
  log(formatReadyLine(readinessResult))

  const hasTcpFailure = tcpResults.some((result) => !result.ok)
  const failed = hasTcpFailure || !readinessResult.ok
  if (failed) {
    logUnhealthyHints(log)
    return { ok: false, tcpResults, readinessResult, exitCode: 1 }
  }

  log('SUT healthcheck OK.')
  return { ok: true, tcpResults, readinessResult, exitCode: 0 }
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    const result = await runSutHealthcheck()
    process.exit(result.exitCode)
  } catch (e) {
    process.stderr.write(`${e instanceof Error ? e.message : String(e)}\n`)
    process.exit(1)
  }
}
