#!/usr/bin/env node
/**
 * Start the Development stack after refusing unsafe checkouts and occupied targets.
 */
import { spawn } from 'node:child_process'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { worktreeIsolationApplies } from './browser-worktree-isolation.mjs'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import {
  listOccupiedApplicationPorts,
  withRuntimeTargetEnv,
} from './local-runtime-target.mjs'
import { isTcpPortOccupied } from './sut-healthcheck.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

const DEVELOPMENT_SERVICES_SCRIPT = path.join(
  repoRoot,
  'scripts/development-services.mjs'
)

function readLivePid(pidFile, isProcessAliveFn) {
  let raw
  try {
    raw = readFileSync(pidFile, 'utf8').trim()
  } catch (error) {
    if (error.code === 'ENOENT') return null
    throw error
  }
  if (raw === '' || !/^[0-9]+$/.test(raw)) return null
  const pid = Number(raw)
  return isProcessAliveFn(pid) ? pid : null
}

function defaultIsProcessAlive(pid) {
  try {
    process.kill(pid, 0)
    return true
  } catch (error) {
    if (error.code === 'ESRCH') return false
    throw error
  }
}

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
 * Refuse unsafe Development starts, then reach the spawn seam.
 *
 * @returns {Promise<number>} exit code (0 = spawn seam reached)
 */
export async function runDevStart({
  checkoutRoot = repoRoot,
  runtimeTarget = DEVELOPMENT_RUNTIME_TARGET,
  spawnFn = spawn,
  isPortOccupiedFn,
  isProcessAliveFn = defaultIsProcessAlive,
  log = (s) => process.stdout.write(`${s}\n`),
} = {}) {
  if (worktreeIsolationApplies(checkoutRoot)) {
    throw new Error(
      'Development (`pnpm dev`) is only supported in the unconfigured primary checkout. ' +
        'This checkout uses worktree isolation; refusing to start. Resources were left unchanged.'
    )
  }

  const livePid = readLivePid(runtimeTarget.pidFile, isProcessAliveFn)
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

  spawnDevelopmentServices({
    spawnFn,
    checkoutRoot,
    runtimeTarget,
    logFile: runtimeTarget.logFile,
  })
  log(
    `Development spawn seam reached for ${runtimeTarget.profile} on LB ${runtimeTarget.lbListenPort}`
  )
  return 0
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
