#!/usr/bin/env node
/**
 * E2E batch runner wrapper: own one SUT stack (and, when the selected spec
 * requires it, one private OpenAI mock + runner lease) for the entire batch,
 * run the selected supported specs through Cypress once, then settle every
 * owned resource and return Cypress's outcome. `pnpm cy:run` and `pnpm test`
 * both enter this wrapper; the wrapper is the single SUT lifecycle owner.
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
import {
  loadCompleteIsolatedE2eAllocation,
  worktreeIsolationApplies,
} from './browser-worktree-isolation.mjs'
import {
  OPEN_AI_SERVICE_LABEL,
  startPrivateOpenAiMock,
} from './isolated-openai-mock.mjs'
import {
  WIKIDATA_SERVICE_LABEL,
  startPrivateWikidataMock,
} from './isolated-wikidata-mock.mjs'
import { acquireSutRunnerLease, releaseSutRunnerLease } from './sut-owner.mjs'
import {
  E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_MOCK_PGID_ENV_KEY,
  E2E_RUNNER_OWNS_LIFETIME_ENV_KEY,
  E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY,
} from './isolated-cypress.mjs'
import { resolveSutCheckoutTarget } from './sut-isolated-target.mjs'
import { listOccupiedApplicationPorts } from './local-runtime-target.mjs'
import { isTcpPortOccupied } from './sut-healthcheck.mjs'
import { stopOwnedSutProcessTree } from './sut-owned-process-tree.mjs'
import {
  SHARED_MOUNTEBANK_MANAGEMENT_PORT,
  SHARED_OPEN_AI_SERVING_PORT,
  SHARED_WIKIDATA_SERVING_PORT,
} from './isolated-mountebank-mock-ports.mjs'
import { assertPortFreeBeforeMockMutation } from './isolated-mountebank-mock-ownership.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

const DEFAULT_CYPRESS_BIN = path.join(
  repoRoot,
  'node_modules/cypress/bin/cypress'
)
const DEFAULT_CYPRESS_CONFIG_FILE = 'e2e_test/config/ci.ts'

function browserFromArgv(argv) {
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--browser' && argv[i + 1]) {
      return argv[i + 1]
    }
    if (typeof arg === 'string' && arg.startsWith('--browser=')) {
      return arg.slice('--browser='.length)
    }
  }
  return
}

function resolveSpecs(argv, checkoutRoot, isolated) {
  if (!hasExplicitCypressSpecSelection(argv)) {
    throw new Error(
      'e2e-runner requires an explicit --spec selection of supported specs.'
    )
  }
  const selected = selectedCypressSpecs({ argv, checkoutRoot })
  if (!isolated) {
    return { specs: selected, approved: null }
  }
  const approved = assertSupportedIsolatedCypressSpecs(selected)
  return { specs: selected, approved }
}

export function defaultSpawnCypress({
  specs,
  cwd,
  env,
  cypressBin,
  configFile,
  stdio,
  browser,
  spawnFn = spawn,
}) {
  const args = ['run', '--config-file', configFile, '--spec', specs.join(',')]
  if (browser) {
    args.push('--browser', browser)
  }
  return spawnFn(process.execPath, [cypressBin, ...args], {
    cwd,
    env,
    stdio,
  })
}

/**
 * Default Cypress `open` (interactive mode) spawner. Invokes the Cypress
 * executable directly (not via a recursive pnpm call) so the wrapper remains
 * the single service owner.
 *
 * `cypress open` does NOT accept `--spec` (that flag is only valid for
 * `cypress run`); spec selection in interactive mode happens in the Cypress
 * UI. An optional preselected `--spec` is still used by the wrapper upstream
 * (via `assertSupportedIsolatedCypressSpecs`) to arrange required session
 * resources — e.g. owning the private OpenAI mock for the session — but it is
 * NOT forwarded to the `cypress open` CLI. The `specs` option is accepted
 * here only so the caller can pass it through unchanged; it is intentionally
 * ignored by the spawn args.
 */
export function defaultSpawnCypressOpen({
  cwd,
  env,
  cypressBin,
  configFile,
  stdio,
  spawnFn = spawn,
}) {
  const args = ['open', '--e2e', '--config-file', configFile]
  return spawnFn(process.execPath, [cypressBin, ...args], {
    cwd,
    env,
    stdio,
  })
}

/**
 * Canonical primary Mountebank management + OpenAI serving ports for a
 * primary-target batch that requires the private OpenAI mock. A foreign
 * listener on either port is refusal — never adoption. Reuses the existing
 * ownership-port check so the refusal message and semantics match the
 * isolated private-mock path.
 */
export async function allocatePrimaryOpenAiMockPorts() {
  const managementPort = SHARED_MOUNTEBANK_MANAGEMENT_PORT
  const servingPort = SHARED_OPEN_AI_SERVING_PORT
  const ownershipOpts = { serviceLabel: OPEN_AI_SERVICE_LABEL }
  await assertPortFreeBeforeMockMutation(
    managementPort,
    'management',
    ownershipOpts
  )
  await assertPortFreeBeforeMockMutation(servingPort, 'serving', ownershipOpts)
  return { managementPort, servingPort }
}

/**
 * Canonical primary Mountebank management + Wikidata serving ports for a
 * primary-target batch that requires the private Wikidata mock. A foreign
 * listener on either port is refusal — never adoption. Reuses the existing
 * ownership-port check so the refusal message and semantics match the
 * isolated private-mock path.
 */
export async function allocatePrimaryWikidataMockPorts() {
  const managementPort = SHARED_MOUNTEBANK_MANAGEMENT_PORT
  const servingPort = SHARED_WIKIDATA_SERVING_PORT
  const ownershipOpts = { serviceLabel: WIKIDATA_SERVICE_LABEL }
  await assertPortFreeBeforeMockMutation(
    managementPort,
    'management',
    ownershipOpts
  )
  await assertPortFreeBeforeMockMutation(servingPort, 'serving', ownershipOpts)
  return { managementPort, servingPort }
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
 * Resolve the invocation checkout target shared by the batch and interactive
 * entry points. `isolated` is determined from the checkout topology (via
 * `isIsolatedCheckoutFn`) without requiring a complete allocation, so a fresh
 * isolated worktree is not forced through `resolveSutCheckoutTarget` (which
 * needs an allocation) before `startOwnedSutLifetime` provisions. For the
 * primary target, the resolved target is computed up front so the wrapper
 * can refuse foreign listeners on its canonical ports before spawning.
 */
function resolveInvocationCheckout({
  checkoutRoot,
  runtimeTarget,
  isIsolatedCheckoutFn,
}) {
  const isolated = isIsolatedCheckoutFn(checkoutRoot)
  const resolvedCheckoutTarget = isolated
    ? undefined
    : resolveSutCheckoutTarget({ checkoutRoot, runtimeTarget })
  return { isolated, resolvedCheckoutTarget }
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
  startPrivateWikidataMockFn = startPrivateWikidataMock,
  cancelEscalationMs,
  isIsolatedCheckoutFn = worktreeIsolationApplies,
  ...lifetimeOpts
} = {}) {
  const { isolated, resolvedCheckoutTarget } = resolveInvocationCheckout({
    checkoutRoot,
    runtimeTarget: lifetimeOpts.runtimeTarget,
    isIsolatedCheckoutFn,
  })
  const browser = browserFromArgv(argv)
  let specs
  let approved
  try {
    const resolved = resolveSpecs(argv, checkoutRoot, isolated)
    specs = resolved.specs
    approved = resolved.approved
  } catch (error) {
    errLog(error.message)
    return 1
  }
  return runOwnedE2eInvocation({
    specs,
    approved,
    spawnCypress,
    checkoutRoot,
    startLifetime,
    cypressBin,
    cypressConfigFile,
    log,
    errLog,
    env,
    stdio,
    cancel,
    startPrivateOpenAiMockFn,
    startPrivateWikidataMockFn,
    label: 'E2E batch',
    cancelEscalationMs,
    browser,
    isolated,
    resolvedCheckoutTarget,
    ...lifetimeOpts,
  })
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

/**
 * Combine several child-exit observers into one: the combined `exited`
 * resolves when ANY observed child exits, and `hasExited()` reports whether
 * any child has already exited. Used when an invocation owns more than one
 * private mock child — a required mock exiting during the run ends it.
 */
function combineChildExits(observers) {
  const valid = observers.filter(Boolean)
  if (valid.length === 0) {
    return null
  }
  return {
    exited: Promise.race(valid.map((o) => o.exited)),
    hasExited: () => valid.some((o) => o.hasExited()),
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
  cancel,
  childExit,
  mockExit = null,
  errLog = (s) => process.stderr.write(`${s}\n`),
  cancelEscalationMs = 5_000,
  browser,
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
        browser,
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
    let childExited = false
    let escalationTimer = null
    const clearEscalation = () => {
      if (escalationTimer) {
        clearTimeout(escalationTimer)
        escalationTimer = null
      }
    }
    const settle = (value) => {
      if (settled) return
      settled = true
      clearEscalation()
      resolve(value)
    }

    // On cancellation, stop the runner (the Cypress child) first; the owned
    // descendants are settled by `lifetime.shutdown()` in the caller's
    // finally. `cypress open` (Electron) does NOT exit on SIGTERM — it prints
    // a graceful-exit message and keeps running — so after signalling SIGTERM,
    // escalate to SIGKILL within a bounded wait if the child has not exited.
    // This unblocks the child-exit await so `lifetime.shutdown()` can run and
    // clean up the owned tree. The normal (non-cancelled) close path is
    // unaffected: no escalation timer is armed unless cancellation fires.
    cancel.onTriggered(() => {
      try {
        child.kill('SIGTERM')
      } catch {
        // already gone
      }
      if (!childExited) {
        escalationTimer = setTimeout(() => {
          if (childExited) return
          try {
            child.kill('SIGKILL')
          } catch {
            // already gone
          }
        }, cancelEscalationMs)
      }
    })

    // A required owned service (the SUT supervisor child, or a private
    // mock) exiting during the test run ends the batch with visible
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
      'Required private mock exited during the test run; ending the batch with failure.'
    )

    child.once('error', (error) => {
      if (settled) return
      clearEscalation()
      reject(error)
    })
    child.once('exit', (code, signal) => {
      childExited = true
      clearEscalation()
      if (signal) settle(1)
      else settle(code ?? 1)
    })
  })
}

/**
 * Run one owned E2E invocation: start the owned SUT (and, when the resolved
 * spec requires it, the owned private OpenAI mock + runner lease), launch
 * Cypress via `spawnCypress`, and settle every owned resource before
 * returning Cypress's outcome. Shared by the batch (`run`) and interactive
 * (`open`) entry points, which differ only in spec resolution (required vs
 * optional) and the Cypress spawn mode. There is no after-spec teardown —
 * the stack and its required mocks persist until Cypress exits, then full
 * cleanup runs via `lifetime.shutdown()` (stop mock + release lease + stop
 * SUT). The plugin acts only as an adapter when the wrapper owns the mock
 * (`E2E_RUNNER_OWNS_LIFETIME=1`); the existing isolated allowlist continues
 * to be enforced for selections the wrapper did not pre-own.
 *
 * The supervisor child exit is observed before awaiting readiness so an exit
 * between readiness and the Cypress run is not missed; this reuses the
 * supervisor's own completion (child exit/close events), not health polling.
 *
 * Cancellation (SIGINT/SIGTERM) and a required service exit (SUT supervisor
 * or owned mock) during the run terminate Cypress and settle the owned tree
 * with a visible nonzero outcome. `label` names the invocation kind in
 * diagnostic messages.
 *
 * @returns {Promise<number>} Cypress's exit outcome (0 = success/clean close, nonzero = failure/cancel).
 */
async function runOwnedE2eInvocation({
  specs,
  approved,
  spawnCypress,
  checkoutRoot,
  startLifetime,
  cypressBin,
  cypressConfigFile,
  log,
  errLog,
  env,
  stdio,
  cancel,
  startPrivateOpenAiMockFn,
  startPrivateWikidataMockFn,
  label,
  cancelEscalationMs,
  browser,
  isolated,
  resolvedCheckoutTarget,
  runtimeTarget,
  isPortOccupiedFn,
  ...lifetimeOpts
}) {
  // Resolve the launch target up front so a primary (unconfigured) checkout
  // can refuse foreign listeners on its canonical application ports BEFORE
  // spawning the supervisor. The isolated target's port check is owned by
  // `startOwnedSutLifetime`; the primary target has no owner/claim gate, so
  // the wrapper guards its canonical ports here. Reuses the existing target
  // rules — no second allowlist or target manager. Callers pre-compute
  // `isolated` and `resolvedCheckoutTarget` via `resolveInvocationCheckout`;
  // for the isolated target `resolvedCheckoutTarget` is undefined (no
  // allocation needed before provisioning).
  const resolvedTarget = resolvedCheckoutTarget?.target
  if (!isolated) {
    const portCheck = isPortOccupiedFn ?? isTcpPortOccupied
    const occupied = await listOccupiedApplicationPorts(
      resolvedTarget,
      portCheck
    )
    if (occupied.length > 0) {
      errLog(
        `Primary SUT ports are already occupied (${occupied.join(', ')}). ` +
          'Refusing to start; the foreign listener was not terminated.'
      )
      cancel.detach()
      return 1
    }
  }

  let lifetime
  try {
    lifetime = await startLifetime({
      checkoutRoot,
      log,
      errLog,
      signal: cancel.signal,
      runtimeTarget,
      ...(isPortOccupiedFn ? { isPortOccupiedFn } : {}),
      ...lifetimeOpts,
    })
  } catch (error) {
    errLog(`Failed to start owned SUT lifetime: ${error.message}`)
    cancel.detach()
    return 1
  }

  const childExit = observeChildExit(lifetime.child)

  // When the resolved specs require private mocks (OpenAI and/or Wikidata),
  // the invocation owns each required mock + a single runner lease for the
  // entire run (no after:spec release). Each mock starts after the SUT is
  // ready and is settled alongside the SUT in the single invocation
  // shutdown. Each service has its own thin adapter over the generic
  // lifecycle; the invocation starts one per required mock.
  const mockHandles = []
  let leaseToken = null
  const mockExitObservers = []
  const originalShutdown = lifetime.shutdown.bind(lifetime)
  lifetime.shutdown = async () => {
    for (const handle of mockHandles) {
      try {
        await handle.stop()
      } catch {
        // best-effort during shutdown
      }
    }
    if (leaseToken) {
      try {
        await releaseSutRunnerLease(checkoutRoot, leaseToken)
      } catch {
        // best-effort during shutdown
      }
    }
    await originalShutdown()
    // Primary target: `startOwnedSutLifetime` claims no SUT owner (the
    // legacy adapter keeps the supervisor running), so its `shutdown()` is
    // a no-op. The wrapper owns the processes it spawns — stop the
    // supervisor child directly via the same owned-tree termination used by
    // the isolated target. No separate shutdown algorithm; the only
    // difference is the launch target.
    if (!isolated) {
      await stopOwnedSutProcessTree(lifetime.child)
    }
  }

  try {
    const ready = await lifetime.ready
    if (!ready.ok) {
      if (cancel.isTriggered()) {
        errLog(`${label} cancelled before SUT became ready.`)
      } else {
        errLog(`SUT readiness failed (exit ${ready.exitCode}).`)
      }
      return 1
    }

    // Collect the required private mocks for this invocation. Each entry
    // carries its starter, its endpoint/pgid env keys, and the primary-target
    // canonical-port allocator. The isolated target coordinates ownership
    // through the SUT owner's runner lease (acquired once per invocation);
    // the primary target has no claimed owner, so the invocation owns each
    // mock directly and uses the canonical serving ports.
    const requiredMocks = []
    if (approved?.requiresPrivateOpenAiMock) {
      requiredMocks.push({
        label: OPEN_AI_SERVICE_LABEL,
        start: startPrivateOpenAiMockFn,
        endpointEnvKey: E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
        pgidEnvKey: E2E_RUNNER_MOCK_PGID_ENV_KEY,
        allocatePrimaryPorts: allocatePrimaryOpenAiMockPorts,
      })
    }
    if (approved?.requiresPrivateWikidataMock) {
      requiredMocks.push({
        label: WIKIDATA_SERVICE_LABEL,
        start: startPrivateWikidataMockFn,
        endpointEnvKey: E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
        pgidEnvKey: E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY,
        allocatePrimaryPorts: allocatePrimaryWikidataMockPorts,
      })
    }

    if (requiredMocks.length > 0 && isolated) {
      try {
        leaseToken = await acquireSutRunnerLease(checkoutRoot)
      } catch (error) {
        errLog(`Failed to acquire runner lease: ${error.message}`)
        return 1
      }
    }

    for (const required of requiredMocks) {
      try {
        const handle = isolated
          ? await required.start({
              checkoutRoot,
              allocation: loadCompleteIsolatedE2eAllocation(checkoutRoot),
            })
          : await required.start({
              checkoutRoot,
              allocation: null,
              allocatePortsFn: required.allocatePrimaryPorts,
            })
        mockHandles.push(handle)
        const observed = observeChildExit(handle.child)
        if (observed) mockExitObservers.push(observed)
      } catch (error) {
        errLog(
          `Failed to start owned private ${required.label} mock: ${error.message}`
        )
        return 1
      }
    }

    const cypressEnv = { ...env }
    if (mockHandles.length > 0) {
      cypressEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY] = '1'
    }
    for (let i = 0; i < mockHandles.length; i++) {
      const handle = mockHandles[i]
      const required = requiredMocks[i]
      cypressEnv[required.endpointEnvKey] = JSON.stringify(handle.endpoint)
      cypressEnv[required.pgidEnvKey] = String(handle.child?.pid ?? '')
    }

    const mockExit = combineChildExits(mockExitObservers)

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
      cancelEscalationMs,
      browser,
    })
    if (cancel.isTriggered()) return 1
    return cypressExitCode
  } catch (error) {
    errLog(`${label} failed: ${error.message}`)
    return 1
  } finally {
    cancel.detach()
    await lifetime.shutdown()
  }
}

/**
 * Run one interactive Cypress session: start the owned SUT (and, when the
 * preselected spec requires it, the owned private OpenAI mock + runner
 * lease), launch Cypress in `open` mode, and keep the single stack alive
 * across every spec selection/rerun the developer makes in the Cypress UI.
 * There is NO after-spec teardown — the stack and its required mocks persist
 * until Cypress closes. When the `open` process exits, full cleanup runs via
 * `lifetime.shutdown()` (stop mock + release lease + stop SUT).
 *
 * Spec selection is not known at launch in pure browsing mode. When the
 * caller supplies an explicit `--spec` that requires a private mock, the
 * wrapper owns the mock for the whole session and injects its endpoint into
 * the Cypress config before the browser launches (the plugin acts only as an
 * adapter via `E2E_RUNNER_OWNS_LIFETIME=1`). Without a preselected mock
 * spec, no mock is started; the existing isolated allowlist continues to be
 * enforced by the plugin for any selection made in the UI.
 *
 * Cancellation (SIGINT/SIGTERM) and a required service exit (SUT supervisor
 * or owned mock) during the session terminate Cypress and settle the owned
 * tree with a visible nonzero outcome.
 *
 * @returns {Promise<number>} Cypress's exit outcome (0 = closed cleanly, nonzero = failure/cancel).
 */
export async function runE2eInteractive({
  argv = process.argv.slice(2),
  checkoutRoot = repoRoot,
  startLifetime = startOwnedSutLifetime,
  spawnCypress = defaultSpawnCypressOpen,
  cypressBin = DEFAULT_CYPRESS_BIN,
  cypressConfigFile = DEFAULT_CYPRESS_CONFIG_FILE,
  log = (s) => process.stdout.write(`${s}\n`),
  errLog = (s) => process.stderr.write(`${s}\n`),
  env = process.env,
  stdio = 'inherit',
  cancel = NO_CANCEL,
  startPrivateOpenAiMockFn = startPrivateOpenAiMock,
  startPrivateWikidataMockFn = startPrivateWikidataMock,
  cancelEscalationMs,
  isIsolatedCheckoutFn = worktreeIsolationApplies,
  ...lifetimeOpts
} = {}) {
  // An interactive session may launch without an explicit --spec (pure
  // browsing). When one is supplied, it must be a single supported spec; if
  // it requires a private mock, the wrapper owns the mock for the session.
  let preselectedSpecs = []
  let approved = null
  if (hasExplicitCypressSpecSelection(argv)) {
    try {
      const selected = selectedCypressSpecs({ argv, checkoutRoot })
      approved = assertSupportedIsolatedCypressSpecs(selected)
      preselectedSpecs = selected
    } catch (error) {
      errLog(error.message)
      return 1
    }
  }
  const { isolated, resolvedCheckoutTarget } = resolveInvocationCheckout({
    checkoutRoot,
    runtimeTarget: lifetimeOpts.runtimeTarget,
    isIsolatedCheckoutFn,
  })
  return runOwnedE2eInvocation({
    specs: preselectedSpecs,
    approved,
    spawnCypress,
    checkoutRoot,
    startLifetime,
    cypressBin,
    cypressConfigFile,
    log,
    errLog,
    env,
    stdio,
    cancel,
    startPrivateOpenAiMockFn,
    startPrivateWikidataMockFn,
    label: 'Interactive E2E session',
    cancelEscalationMs,
    isolated,
    resolvedCheckoutTarget,
    ...lifetimeOpts,
  })
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  const cancel = wireBatchCancellation()
  const interactive = process.argv.includes('--open')
  const code = interactive
    ? await runE2eInteractive({ cancel })
    : await runE2eBatch({ cancel })
  process.exit(code)
}
