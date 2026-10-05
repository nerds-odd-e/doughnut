#!/usr/bin/env node
/**
 * Start the Development stack after refusing unsafe checkouts and occupied targets.
 * Spawns services, waits until healthy `dev`, prints browser origin.
 */
import { spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { runDevelopmentHealthcheck } from './dev-healthcheck.mjs'
import { refuseDevelopmentInLinkedWorktree } from './development-primary-checkout.mjs'
import {
  DEVELOPMENT_RUNTIME_TARGET,
  developmentServicesScript,
} from './development-runtime.mjs'
import { findDevelopmentServicesPids } from './development-stack-processes.mjs'
import {
  browserOrigin,
  listOccupiedApplicationPorts,
  withRuntimeTargetEnv,
} from './local-runtime-target.mjs'
import { isTcpPortOccupied } from './sut-healthcheck.mjs'
import { waitForSutHealthy } from './sut-start-health-wait.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

export function spawnDevelopmentServices({
  spawnFn = spawn,
  checkoutRoot = repoRoot,
  runtimeTarget = DEVELOPMENT_RUNTIME_TARGET,
  logFile = runtimeTarget.logFile,
} = {}) {
  const child = spawnFn(
    process.execPath,
    [developmentServicesScript(checkoutRoot)],
    {
      cwd: checkoutRoot,
      detached: true,
      env: withRuntimeTargetEnv(
        { ...process.env, DEV_LOG_FILE: logFile },
        runtimeTarget
      ),
      stdio: 'ignore',
      shell: false,
    }
  )
  child.unref?.()
  return { child, logFile }
}

/**
 * Refuse unsafe Development starts, spawn services, wait until healthy.
 *
 * @returns {Promise<number>} exit code (0 = healthy)
 */
export async function runDevStart({
  checkoutRoot = repoRoot,
  runtimeTarget = DEVELOPMENT_RUNTIME_TARGET,
  spawnFn = spawn,
  isPortOccupiedFn,
  healthcheckFn = runDevelopmentHealthcheck,
  timeoutMs,
  pollMs,
  log = (s) => process.stdout.write(`${s}\n`),
  errLog = (s) => process.stderr.write(`${s}\n`),
} = {}) {
  refuseDevelopmentInLinkedWorktree(
    checkoutRoot,
    'pnpm dev',
    'refusing to start'
  )

  const runningPids = await findDevelopmentServicesPids(checkoutRoot)
  if (runningPids.length > 0) {
    throw new Error(
      `Development is already running (services pid ${runningPids.join(', ')}). ` +
        'Refusing to start; resources were left unchanged.'
    )
  }

  const occupied = await listOccupiedApplicationPorts(
    runtimeTarget,
    isPortOccupiedFn ?? isTcpPortOccupied
  )
  if (occupied.length > 0) {
    throw new Error(
      `Development ports are already occupied (${occupied.join(', ')}). ` +
        'Refusing to start; the listener was not terminated.'
    )
  }

  const logFile = runtimeTarget.logFile
  log(`Starting Development services... (log: ${logFile})`)
  const { child } = spawnDevelopmentServices({
    spawnFn,
    checkoutRoot,
    runtimeTarget,
    logFile,
  })

  const { exitCode } = await waitForSutHealthy({
    child,
    timeoutMs,
    pollMs,
    logFile,
    log,
    errLog,
    healthcheckFn,
    runtimeTarget,
    checkoutRoot,
    stackLabel: 'Development',
  })
  if (exitCode === 0) {
    log(`Browser origin: ${browserOrigin(runtimeTarget)}`)
  }
  return exitCode
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    const code = await runDevStart()
    process.exit(code)
  } catch (error) {
    process.stderr.write(
      `${error instanceof Error ? error.message : String(error)}\n`
    )
    process.exit(1)
  }
}
