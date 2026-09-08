#!/usr/bin/env node
/**
 * Stop the unconfigured primary SUT listeners on 5173 / 5174 / 9081
 * (not mountebank), then run `pnpm sut`. Isolated checkouts ask the live
 * owner to stop its children and start again on the recorded allocation.
 * Run: `CURSOR_DEV=true nix develop -c pnpm sut:restart`
 */
import { execFile, spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  refuseUnsupportedIsolatedBrowserCommand,
  worktreeIsolationApplies,
} from './browser-worktree-isolation.mjs'
import {
  beginSutOwnerShutdown,
  holdSutOwnershipAcrossRestart,
  verifyLiveSutOwner,
} from './sut-owner.mjs'
import { getListenerPids } from './sut-listener-pids.mjs'
import { runSutStart } from './sut-start.mjs'

/** Ports used by `pnpm sut` except mountebank (2525). See docs/gcp/prod_env.md */
export const SUT_RESTART_PORTS = [5173, 5174, 9081]

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

/**
 * @param {number} port
 * @param {{ execFileFn?: typeof execFile }} [deps]
 * @returns {Promise<number[]>} PIDs signalled (may include already-dead)
 */
export async function terminateTcpListenersOnPort(port, deps) {
  const pids = await getListenerPids(port, deps)
  for (const pid of pids) {
    try {
      process.kill(pid, 'SIGTERM')
    } catch (e) {
      if (e.code !== 'ESRCH') throw e
    }
  }
  return pids
}

/**
 * @param {{ log?: (s: string) => void, ports?: number[], execFileFn?: typeof execFile, spawnFn?: typeof spawn }} [opts]
 */
export async function stopSutPorts({
  log = console.log,
  ports = SUT_RESTART_PORTS,
  execFileFn = execFile,
} = {}) {
  for (const port of ports) {
    const pids = await getListenerPids(port, { execFileFn })
    if (pids.length === 0) {
      log(`port ${port}: no TCP listener`)
    } else {
      log(`port ${port}: sending SIGTERM to PID(s) ${pids.join(', ')}`)
      for (const pid of pids) {
        try {
          process.kill(pid, 'SIGTERM')
        } catch (e) {
          if (e.code !== 'ESRCH') throw e
        }
      }
    }
  }
}

/**
 * @param {{ cwd?: string, spawnFn?: typeof spawn }} [opts]
 * @returns {Promise<number>} exit code of `pnpm sut`
 */
export function startPnpmSut({ cwd = repoRoot, spawnFn = spawn } = {}) {
  return new Promise((resolve, reject) => {
    const child = spawnFn('pnpm', ['sut'], {
      cwd,
      stdio: 'inherit',
      shell: false,
    })
    child.on('error', (e) => {
      reject(
        new Error(
          e.code === 'ENOENT'
            ? 'pnpm not found on PATH; use the repo dev shell or install pnpm.'
            : e.message
        )
      )
    })
    child.on('close', (code, signal) => {
      if (signal) {
        resolve(1)
        return
      }
      resolve(code ?? 1)
    })
  })
}

function pause(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitUntilLiveOwnerStops(checkoutRoot, timeoutMs) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (!(await verifyLiveSutOwner(checkoutRoot)).ok) return
    await pause(40)
  }
  throw new Error(
    'Timed out waiting for the isolated SUT owner to stop its children. Listeners were not discovered by port.'
  )
}

async function restartIsolatedSutOwner({
  checkoutRoot,
  ownerStopTimeoutMs = 15_000,
  ...startOpts
}) {
  const live = await verifyLiveSutOwner(checkoutRoot)
  if (!live.ok) {
    throw new Error(
      'Isolated restart requires a verified live SUT owner in this checkout. ' +
        'A stale or unverifiable owner was left unchanged; listeners were not signalled.'
    )
  }
  await holdSutOwnershipAcrossRestart(checkoutRoot)
  await beginSutOwnerShutdown(checkoutRoot)
  await waitUntilLiveOwnerStops(checkoutRoot, ownerStopTimeoutMs)
  return runSutStart({
    ...startOpts,
    checkoutRoot,
    retainOwnership: true,
  })
}

export async function runSutRestart(opts = {}) {
  const checkoutRoot = opts.checkoutRoot ?? repoRoot
  refuseUnsupportedIsolatedBrowserCommand({
    checkoutRoot,
    command: 'pnpm sut:restart',
  })
  if (worktreeIsolationApplies(checkoutRoot)) {
    return restartIsolatedSutOwner({ ...opts, checkoutRoot })
  }
  await stopSutPorts(opts)
  return startPnpmSut({ cwd: opts.cwd ?? repoRoot, spawnFn: opts.spawnFn })
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    const code = await runSutRestart()
    process.exit(code)
  } catch (e) {
    console.error(e instanceof Error ? e.message : e)
    process.exit(1)
  }
}
