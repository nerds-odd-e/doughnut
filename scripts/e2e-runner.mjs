#!/usr/bin/env node
/**
 * E2E batch runner wrapper: own one SUT stack (and, when the selected spec
 * requires it, one private OpenAI mock + runner lease) for the entire batch,
 * run the selected supported specs through Cypress once, then settle every
 * owned resource and return Cypress's outcome. Existing `pnpm cy:run` /
 * `pnpm sut` commands remain usable until the migration slice.
 *
 * The wrapper invokes the Cypress executable directly (not via a recursive
 * pnpm call) to avoid a second service owner. Ownership is claimed by the
 * owned lifetime before Cypress is signalled. The Cypress plugin acts only as
 * an adapter when the wrapper owns the mock (signalled via env), injecting
 * the owned endpoint into `expose` and registering read-only tasks.
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
import { loadCompleteIsolatedE2eAllocation } from './browser-worktree-isolation.mjs'
import { startPrivateOpenAiMock } from './isolated-openai-mock.mjs'
import { acquireSutRunnerLease, releaseSutRunnerLease } from './sut-owner.mjs'
import {
  E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_MOCK_PGID_ENV_KEY,
  E2E_RUNNER_OWNS_LIFETIME_ENV_KEY,
} from './isolated-cypress.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

const DEFAULT_CYPRESS_BIN = path.join(
  repoRoot,
  'node_modules/cypress/bin/cypress'
)
const DEFAULT_CYPRESS_CONFIG_FILE = 'e2e_test/config/ci.ts'

function resolveSpecs(argv, checkoutRoot) {
  if (!hasExplicitCypressSpecSelection(argv)) {
    throw new Error(
      'e2e-runner requires an explicit --spec selection of one supported spec.'
    )
  }
  const selected = selectedCypressSpecs({ argv, checkoutRoot })
  const approved = assertSupportedIsolatedCypressSpecs(selected)
  return { specs: selected, approved }
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
  startPrivateOpenAiMockFn = startPrivateOpenAiMock,
  ...lifetimeOpts
} = {}) {
  let specs
  let approved
  try {
    const resolved = resolveSpecs(argv, checkoutRoot)
    specs = resolved.specs
    approved = resolved.approved
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

  // When the batch requires a private OpenAI mock, the invocation owns the
  // mock + runner lease for the entire batch (no after:spec release). The
  // mock starts after the SUT is ready and is settled alongside the SUT in
  // the single invocation shutdown.
  const mockState = { handle: null, leaseToken: null }
  let mockExit = null
  const originalShutdown = lifetime.shutdown.bind(lifetime)
  lifetime.shutdown = async () => {
    if (mockState.handle) {
      try {
        await mockState.handle.stop()
      } catch {
        // best-effort during shutdown
      }
    }
    if (mockState.leaseToken) {
      try {
        await releaseSutRunnerLease(checkoutRoot, mockState.leaseToken)
      } catch {
        // best-effort during shutdown
      }
    }
    await originalShutdown()
  }

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

    if (approved.requiresPrivateOpenAiMock) {
      try {
        mockState.leaseToken = await acquireSutRunnerLease(checkoutRoot)
      } catch (error) {
        errLog(`Failed to acquire runner lease: ${error.message}`)
        return 1
      }
      try {
        const allocation = loadCompleteIsolatedE2eAllocation(checkoutRoot)
        mockState.handle = await startPrivateOpenAiMockFn({
          checkoutRoot,
          allocation,
        })
        mockExit = observeChildExit(mockState.handle.child)
      } catch (error) {
        errLog(`Failed to start owned private OpenAI mock: ${error.message}`)
        return 1
      }
    }

    const cypressEnv = { ...env }
    if (mockState.handle) {
      cypressEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY] = '1'
      cypressEnv[E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY] = JSON.stringify(
        mockState.handle.endpoint
      )
      cypressEnv[E2E_RUNNER_MOCK_PGID_ENV_KEY] = String(
        mockState.handle.child?.pid ?? ''
      )
    }

    const cypressExitCode = await runCypressOnce({
      specs,
      spawnCypress,
      cypressBin,
      cypressConfigFile,
      checkoutRoot,
      env: cypressEnv,
      stdio,
      cancel,
      childExit,
      mockExit,
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
  mockExit = null,
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

    // A required owned service (the SUT supervisor child, or the private
    // OpenAI mock) exiting during the test run ends the batch with visible
    // failure. Terminate Cypress promptly — do not wait for it to finish on
    // its own — then let the caller's `lifetime.shutdown()` settle the
    // remaining owned tree. Covers the timing window: if the child already
    // exited between readiness and this attachment, end the run now.
    const endRunOnRequiredExit = (exit, message) => {
      if (!exit) return
      const onExit = () => {
        if (settled) return
        errLog(message)
        try {
          child.kill('SIGTERM')
        } catch {
          // already gone
        }
        settle(1)
      }
      if (exit.hasExited()) {
        onExit()
      } else {
        exit.exited.then(onExit)
      }
    }
    endRunOnRequiredExit(
      childExit,
      'Required SUT service exited during the test run; ending the batch with failure.'
    )
    endRunOnRequiredExit(
      mockExit,
      'Required private OpenAI mock exited during the test run; ending the batch with failure.'
    )

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
