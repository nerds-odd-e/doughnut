#!/usr/bin/env node
/**
 * Stop this checkout's Development stack: every running Development services
 * process started from this checkout, found in the process table, with its tree.
 */
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { stopOwnedDevelopmentProcessTree } from './development-owned-process-tree.mjs'
import { refuseDevelopmentInLinkedWorktree } from './development-primary-checkout.mjs'
import { developmentServicesScript } from './development-runtime.mjs'
import { listProcessTable } from './worktree-retirement-checkout-processes.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

export async function findDevelopmentServicesPids(checkoutRoot) {
  const script = developmentServicesScript(checkoutRoot)
  const rows = await listProcessTable()
  return rows
    .filter((row) => row.command.includes(script))
    .map((row) => row.pid)
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

  const pids = await findDevelopmentServicesPids(checkoutRoot)
  if (pids.length === 0) {
    log('Development is not running.')
    return 0
  }
  for (const pid of pids) {
    await stopOwnedDevelopmentProcessTree(pid)
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
