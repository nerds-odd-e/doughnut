#!/usr/bin/env node
/**
 * Start the Development stack after refusing unsafe checkouts and occupied targets.
 * Spawns services, writes `dev.pid`, waits until healthy `dev`, prints browser origin.
 */
import { spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { worktreeIsolationApplies } from './browser-worktree-isolation.mjs'
import { runDevelopmentHealthcheck } from './dev-healthcheck.mjs'
import { isProcessAlive, readLiveDevelopmentPid } from './development-pid.mjs'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import {
  browserOrigin,
  listOccupiedApplicationPorts,
  withRuntimeTargetEnv,
} from './local-runtime-target.mjs'
import { isTcpPortOccupied } from './sut-healthcheck.mjs'
import { waitForSutHealthy } from './sut-start-health-wait.mjs'
import { writePidFile } from './sut-start-spawn.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

const DEVELOPMENT_SERVICES_SCRIPT = path.join(
  repoRoot,
  'scripts/development-services.mjs'
)

export function spawnDevelopmentServices({
  spawnFn = spawn,
  checkoutRoot = repoRoot,
  runtimeTarget = DEVELOPMENT_RUNTIME_TARGET,
  logFile = runtimeTarget.logFile,
} = {}) {
  const child = spawnFn(process.execPath, [DEVELOPMENT_SERVICES_SCRIPT], {
    cwd: checkoutRoot,
    detached: true,
    env: withRuntimeTargetEnv(
      { ...process.env, DEV_LOG_FILE: logFile },
      runtimeTarget
    ),
    stdio: 'ignore',
    shell: false,
  })
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
  isProcessAliveFn = isProcessAlive,
  healthcheckFn = runDevelopmentHealthcheck,
  timeoutMs,
  pollMs,
  log = (s) => process.stdout.write(`${s}\n`),
  errLog = (s) => process.stderr.write(`${s}\n`),
} = {}) {
  if (worktreeIsolationApplies(checkoutRoot)) {
    throw new Error(
      'Development (`pnpm dev`) is only supported in the unconfigured primary checkout. ' +
        'This checkout uses worktree isolation; refusing to start. Resources were left unchanged.'
    )
  }

  const livePid = readLiveDevelopmentPid(
    runtimeTarget.pidFile,
    isProcessAliveFn
  )
  if (livePid !== null) {
    throw new Error(
      `Development is already running (live process ${livePid} in ${runtimeTarget.pidFile}). ` +
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
  await writePidFile(child.pid, { pidFile: runtimeTarget.pidFile })

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
