#!/usr/bin/env node
/**
 * Development stack healthcheck for `pnpm dev`.
 * TCP + LB readiness + `/api/healthcheck` body must report Active Profile `dev`.
 * Does not alter SUT healthcheck behavior.
 */
import http from 'node:http'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import { browserOrigin, healthEndpoints } from './local-runtime-target.mjs'
import {
  checkHttpReady,
  checkTcpPort,
  formatReadyLine,
  formatTcpLine,
} from './sut-healthcheck.mjs'

const HTTP_TIMEOUT_MS = 10_000

export function checkHttpBody({ url, timeoutMs = HTTP_TIMEOUT_MS }) {
  return new Promise((resolve) => {
    const req = http.get(url, { timeout: timeoutMs }, (res) => {
      const chunks = []
      res.on('data', (chunk) => chunks.push(chunk))
      res.on('end', () => {
        const status = res.statusCode ?? 0
        const body = Buffer.concat(chunks).toString('utf8')
        resolve({
          ok: status >= 200 && status < 300,
          status,
          body,
        })
      })
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

export function activeProfilesIncludeDev(body) {
  const match = String(body).match(/Active Profile:\s*([^.]+)/)
  if (!match) return false
  return match[1]
    .split(',')
    .map((part) => part.trim())
    .includes('dev')
}

function formatProfileLine(result) {
  if (result.skipped)
    return `SKIP Active Profile ${result.url} - ${result.reason}`
  if (result.ok) {
    return `PASS Active Profile ${result.url} - ${result.body}`
  }
  if (result.body !== undefined) {
    return `FAIL Active Profile ${result.url} - expected Active Profile: dev; got ${result.body}`
  }
  return `FAIL Active Profile ${result.url} - ${result.reason}`
}

function logUnhealthyHints(log) {
  log('Development unhealthy or still starting.')
  log('If you just started `pnpm dev`, wait a few seconds and run again.')
  log('If services are down, start with `pnpm dev`.')
}

/**
 * @param {{
 *   runtimeTarget?: typeof DEVELOPMENT_RUNTIME_TARGET,
 *   tcpChecks?: Array<{ service: string, host: string, port: number }>,
 *   readinessUrl?: string,
 *   healthcheckUrl?: string,
 *   log?: (s: string) => void,
 * }} [opts]
 */
export async function runDevelopmentHealthcheck({
  runtimeTarget = DEVELOPMENT_RUNTIME_TARGET,
  tcpChecks,
  readinessUrl,
  healthcheckUrl,
  log = console.log,
} = {}) {
  const endpoints = healthEndpoints(runtimeTarget)
  const checks = tcpChecks ?? endpoints.tcpChecks
  const readyUrl = readinessUrl ?? endpoints.readinessUrl
  const profileUrl =
    healthcheckUrl ?? `${browserOrigin(runtimeTarget)}/api/healthcheck`

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

  let profileResult
  if (!readinessResult.ok) {
    profileResult = {
      skipped: true,
      url: profileUrl,
      reason: 'LB readiness not OK',
    }
  } else {
    const fetched = await checkHttpBody({ url: profileUrl })
    profileResult = {
      url: profileUrl,
      ...fetched,
      ok: Boolean(fetched.ok && activeProfilesIncludeDev(fetched.body)),
    }
  }
  log(formatProfileLine(profileResult))

  const hasTcpFailure = tcpResults.some((result) => !result.ok)
  const failed = hasTcpFailure || !readinessResult.ok || !profileResult.ok
  if (failed) {
    logUnhealthyHints(log)
    return {
      ok: false,
      tcpResults,
      readinessResult,
      profileResult,
      exitCode: 1,
    }
  }

  log('Development healthcheck OK.')
  return {
    ok: true,
    tcpResults,
    readinessResult,
    profileResult,
    exitCode: 0,
  }
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  runDevelopmentHealthcheck()
    .then((result) => {
      process.exit(result.exitCode)
    })
    .catch((e) => {
      process.stderr.write(`${e instanceof Error ? e.message : String(e)}\n`)
      process.exit(1)
    })
}
