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
 * No-op cancellation handle. The default for `runE2eBatch` so callers that do
 * not need signal handling install no process listeners. The `isMain` entry
 * point wires real SIGINT/SIGTERM handling via `wireBatchCancellation`.
 */
const NO_CANCEL = {
  signal: undefined,
  isTriggered: () => false,
  onTriggered: () => () => undefined,
  detach: () => undefined,
}

/**
 * Wire SIGINT/SIGTERM to a single AbortController for one batch invocation.
 * The controller's signal aborts the readiness wait; `onTriggered` callbacks
 * stop the Cypress child. One cancellation path — no generic signal framework.
 */
export function wireBatchCancellation(signals = ['SIGINT', 'SIGTERM']) {
  const controller = new AbortController()
  const onSignal = () => controller.abort()
  for (const sig of signals) process.once(sig, onSignal)
  return {
    signal: controller.signal,
    isTriggered: () => controller.signal.aborted,
    onTriggered: (cb) => {
      if (controller.signal.aborted) {
        cb()
        return () => undefined
      }
      controller.signal.addEventListener('abort', cb, { once: true })
      return () => controller.signal.removeEventListener('abort', cb)
    },
    detach: () => {
      for (const sig of signals) process.off(sig, onSignal)
    },
  }
}

/**
 * Run one E2E batch: start the owned SUT, wait for readiness, run the selected
 * supported no-mock specs through Cypress once, then settle the owned tree.
 *
 * Cancellation: when the supplied `cancel` is triggered (SIGINT/SIGTERM in
 * production), the readiness wait is aborted and the running Cypress child is
 * signalled to stop. The runner then awaits the existing `lifetime.shutdown()`
 * (the shared real owned-tree termination + ownership release) before
 * returning a visible nonzero outcome. Ownership is not released before the
 * owned tree is actually stopped; a cleanup failure propagates rather than
 * being masked as success.
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
  cancel = NO_CANCEL,
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
      signal: cancel.signal,
      ...lifetimeOpts,
    })
  } catch (error) {
    errLog(`Failed to start owned SUT lifetime: ${error.message}`)
    cancel.detach()
    return 1
  }

  // Observe the supervisor child exit promptly. The observer is attached
  // before awaiting readiness so an exit between readiness and the Cypress
  // run is not missed. This reuses the supervisor's own completion (the
  // child handle's exit/close events); it is not a health-polling loop.
  const childExit = observeChildExit(lifetime.child)

  try {
    const ready = await lifetime.ready
    if (!ready.ok) {
      if (cancel.isTriggered()) {
        errLog('E2E batch cancelled before SUT became ready.')
      } else {
        errLog(`SUT readiness failed (exit ${ready.exitCode}).`)
      }
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
      cancel,
      childExit,
      errLog,
    })
    if (cancel.isTriggered()) return 1
    return cypressExitCode
  } catch (error) {
    errLog(`E2E batch failed: ${error.message}`)
    return 1
  } finally {
    cancel.detach()
    await lifetime.shutdown()
  }
}

/**
 * Observe the supervisor child's exit without polling. Returns a handle
 * whose `exited` promise resolves `true` when the child has exited, and
 * `hasExited()` reports whether the exit was already observed. The promise
 * is created synchronously so an exit in the readiness-to-run window is
 * captured before the Cypress run attaches its own listener.
 */
function observeChildExit(child) {
  if (!child || typeof child.on !== 'function') {
    // No observable child (test mock): nothing to observe, so `exited` never
    // resolves and `hasExited()` stays false — no spurious service-exit path.
    return {
      exited: new Promise(() => {
        /* never resolves */
      }),
      hasExited: () => false,
    }
  }
  let exited = false
  const promise = new Promise((resolve) => {
    if (child.exitCode != null || child.signalCode) {
      exited = true
      resolve(true)
      return
    }
    child.once('exit', () => {
      exited = true
      resolve(true)
    })
  })
  return { exited: promise, hasExited: () => exited }
}

function runCypressOnce({
  specs,
  spawnCypress,
  cypressBin,
  cypressConfigFile,
  checkoutRoot,
  env,
  stdio,
  cancel,
  childExit,
  errLog = (s) => process.stderr.write(`${s}\n`),
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

    let settled = false
    const settle = (value) => {
      if (settled) return
      settled = true
      resolve(value)
    }

    // On cancellation, stop the runner (the Cypress child) first; the owned
    // descendants are settled by `lifetime.shutdown()` in the caller's finally.
    cancel.onTriggered(() => {
      try {
        child.kill('SIGTERM')
      } catch {
        // already gone
      }
    })

    // A required application service (the supervisor child) exiting during
    // the test run ends the batch with visible failure. Terminate Cypress
    // promptly — do not wait for it to finish on its own — then let the
    // caller's `lifetime.shutdown()` settle the remaining owned tree.
    // Covers the timing window: if the child already exited between readiness
    // and this attachment, end the run now.
    const onChildExit = () => {
      if (settled) return
      errLog(
        'Required SUT service exited during the test run; ending the batch with failure.'
      )
      try {
        child.kill('SIGTERM')
      } catch {
        // already gone
      }
      settle(1)
    }
    if (childExit.hasExited()) {
      onChildExit()
    } else {
      childExit.exited.then(onChildExit)
    }

    child.once('error', (error) => {
      if (settled) return
      reject(error)
    })
    child.once('exit', (code, signal) => {
      if (signal) settle(1)
      else settle(code ?? 1)
    })
  })
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  const cancel = wireBatchCancellation()
  const code = await runE2eBatch({ cancel })
  process.exit(code)
}
