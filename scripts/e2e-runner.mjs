#!/usr/bin/env node
/**
 * E2E batch runner wrapper: own one SUT stack, run selected supported no-mock
 * specs through Cypress once, then settle the owned tree and return Cypress's
 * outcome. Existing `pnpm cy:run` / `pnpm sut` commands remain usable until
 * the migration slice.
 *
 * The wrapper invokes the Cypress executable directly (not via a recursive
 * pnpm call) to avoid a second service owner. Ownership is claimed by the
 * owned lifetime before Cypress is signalled.
 */
import { spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  assertSupportedIsolatedCypressSpecs,
  hasExplicitCypressSpecSelection,
  selectedCypressSpecs,
} from './isolated-cypress-spec-selection.mjs'
import { startOwnedSutLifetime } from './sut-start.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

const DEFAULT_CYPRESS_BIN = path.join(
  repoRoot,
  'node_modules/cypress/bin/cypress'
)
const DEFAULT_CYPRESS_CONFIG_FILE = 'e2e_test/config/ci.ts'

function resolveNoMockSpecs(argv, checkoutRoot) {
  if (!hasExplicitCypressSpecSelection(argv)) {
    throw new Error(
      'e2e-runner requires an explicit --spec selection of one supported no-mock spec.'
    )
  }
  const selected = selectedCypressSpecs({ argv, checkoutRoot })
  const approved = assertSupportedIsolatedCypressSpecs(selected)
  if (approved.requiresPrivateOpenAiMock) {
    throw new Error(
      'e2e-runner (slice 3) only runs no-mock specs; private-mock specs arrive in a later slice.'
    )
  }
  return selected
}

function defaultSpawnCypress({
  specs,
  cwd,
  env,
  cypressBin,
  configFile,
  stdio,
}) {
  const args = ['run', '--config-file', configFile, '--spec', specs.join(',')]
  return spawn(process.execPath, [cypressBin, ...args], {
    cwd,
    env,
    stdio,
  })
}

/**
 * Run one E2E batch: start the owned SUT, wait for readiness, run the selected
 * supported no-mock specs through Cypress once, then settle the owned tree.
 *
 * @returns {Promise<number>} Cypress's exit outcome (0 = success, nonzero = failure).
 */
export async function runE2eBatch({
  argv = process.argv.slice(2),
  checkoutRoot = repoRoot,
  startLifetime = startOwnedSutLifetime,
  spawnCypress = defaultSpawnCypress,
  cypressBin = DEFAULT_CYPRESS_BIN,
  cypressConfigFile = DEFAULT_CYPRESS_CONFIG_FILE,
  log = (s) => process.stdout.write(`${s}\n`),
  errLog = (s) => process.stderr.write(`${s}\n`),
  env = process.env,
  stdio = 'inherit',
  ...lifetimeOpts
} = {}) {
  let specs
  try {
    specs = resolveNoMockSpecs(argv, checkoutRoot)
  } catch (error) {
    errLog(error.message)
    return 1
  }

  let lifetime
  try {
    lifetime = await startLifetime({
      checkoutRoot,
      log,
      errLog,
      ...lifetimeOpts,
    })
  } catch (error) {
    errLog(`Failed to start owned SUT lifetime: ${error.message}`)
    return 1
  }

  try {
    const ready = await lifetime.ready
    if (!ready.ok) {
      errLog(`SUT readiness failed (exit ${ready.exitCode}).`)
      return 1
    }

    const cypressExitCode = await runCypressOnce({
      specs,
      spawnCypress,
      cypressBin,
      cypressConfigFile,
      checkoutRoot,
      env,
      stdio,
    })
    return cypressExitCode
  } catch (error) {
    errLog(`E2E batch failed: ${error.message}`)
    return 1
  } finally {
    await lifetime.shutdown()
  }
}

function runCypressOnce({
  specs,
  spawnCypress,
  cypressBin,
  cypressConfigFile,
  checkoutRoot,
  env,
  stdio,
}) {
  return new Promise((resolve, reject) => {
    let child
    try {
      child = spawnCypress({
        specs,
        cypressBin,
        configFile: cypressConfigFile,
        cwd: checkoutRoot,
        env,
        stdio,
      })
    } catch (error) {
      reject(error)
      return
    }
    if (!child || typeof child.on !== 'function') {
      reject(new Error('spawnCypress did not return a child process.'))
      return
    }
    child.once('error', (error) => reject(error))
    child.once('exit', (code, signal) => {
      if (signal) resolve(1)
      else resolve(code ?? 1)
    })
  })
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  const code = await runE2eBatch()
  process.exit(code)
}
