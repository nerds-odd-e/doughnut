#!/usr/bin/env node
/**
 * Stop this checkout's Development stack: every running Development services
 * process started from this checkout, found in the process table, with its tree.
 */
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { isProcessAlive } from './development-pid.mjs'
import { refuseDevelopmentInLinkedWorktree } from './development-primary-checkout.mjs'
import { developmentServicesScript } from './development-runtime.mjs'
import { terminateOwnedProcessTree } from './owned-process-tree-termination.mjs'
import { descendantPidsByParentWalk } from './sut-listener-pids.mjs'
import { listProcessTable } from './worktree-retirement-checkout-processes.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

async function findDevelopmentServicesPids(checkoutRoot) {
  const script = developmentServicesScript(checkoutRoot)
  const rows = await listProcessTable()
  return rows
    .filter((row) => row.command.includes(script))
    .map((row) => row.pid)
}

function signalTarget(target, signal) {
  try {
    process.kill(target, signal)
  } catch (error) {
    if (error.code !== 'ESRCH' && error.code !== 'EPERM') throw error
  }
}

function servicesTreeStillRunning(servicesPid, descendants) {
  return (
    isProcessAlive(servicesPid) ||
    isProcessAlive(-servicesPid) ||
    descendants.some((pid) => isProcessAlive(pid))
  )
}

function signalServicesTree(servicesPid, descendants, signal) {
  for (const pid of descendants) {
    signalTarget(pid, signal)
  }
  signalTarget(servicesPid, signal)
  signalTarget(-servicesPid, signal)
}

/**
 * SIGTERM, then SIGKILL if needed, the services process, its process group,
 * and its parent-walk descendants.
 */
async function stopServicesTree(servicesPid) {
  await terminateOwnedProcessTree({
    captureDescendants: () => descendantPidsByParentWalk(servicesPid),
    signalOwnedTree: (descendants, signal) =>
      signalServicesTree(servicesPid, descendants, signal),
    isOwnedTreeRunning: (descendants) =>
      servicesTreeStillRunning(servicesPid, descendants),
    failureMessage:
      'Development services process tree did not exit after SIGKILL within the bounded wait',
  })
}

/**
 * Stop every Development services process tree started from `checkoutRoot`.
 *
 * @returns {Promise<number[]>} the services PIDs stopped
 */
export async function stopDevelopmentServices(checkoutRoot) {
  const pids = await findDevelopmentServicesPids(checkoutRoot)
  for (const pid of pids) {
    await stopServicesTree(pid)
  }
  return pids
}

/**
 * @returns {Promise<number>} exit code (0 = stopped or not running)
 */
export async function runDevStop({
  checkoutRoot = repoRoot,
  log = (s) => process.stdout.write(`${s}\n`),
} = {}) {
  refuseDevelopmentInLinkedWorktree(
    checkoutRoot,
    'pnpm dev:stop',
    'refusing to stop'
  )

  const pids = await stopDevelopmentServices(checkoutRoot)
  if (pids.length === 0) {
    log('Development is not running.')
    return 0
  }
  log(`Stopped Development services (pid ${pids.join(', ')}).`)
  return 0
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    process.exit(await runDevStop())
  } catch (error) {
    process.stderr.write(
      `${error instanceof Error ? error.message : String(error)}\n`
    )
    process.exit(1)
  }
}
