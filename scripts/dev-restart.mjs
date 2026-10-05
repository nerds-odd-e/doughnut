#!/usr/bin/env node
/**
 * Restart the Development stack: stop this checkout's Development services
 * (as `pnpm dev:stop` does), then start (as `pnpm dev` does).
 */
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { runDevStart } from './dev-start.mjs'
import { refuseDevelopmentInLinkedWorktree } from './development-primary-checkout.mjs'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import { stopDevelopmentServices } from './development-stack-processes.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

/**
 * @returns {Promise<number>}
 */
export async function runDevRestart({
  checkoutRoot = repoRoot,
  runtimeTarget = DEVELOPMENT_RUNTIME_TARGET,
  runDevStartFn = runDevStart,
} = {}) {
  refuseDevelopmentInLinkedWorktree(
    checkoutRoot,
    'pnpm dev:restart',
    'refusing restart'
  )
  await stopDevelopmentServices(checkoutRoot)
  return runDevStartFn({ checkoutRoot, runtimeTarget })
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    const code = await runDevRestart()
    process.exit(code)
  } catch (error) {
    process.stderr.write(
      `${error instanceof Error ? error.message : String(error)}\n`
    )
    process.exit(1)
  }
}
