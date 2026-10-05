#!/usr/bin/env node
/** Stop this checkout's Development stack (`pnpm dev:stop`). */
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { refuseDevelopmentInLinkedWorktree } from './development-primary-checkout.mjs'
import { stopDevelopmentServices } from './development-stack-processes.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

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
