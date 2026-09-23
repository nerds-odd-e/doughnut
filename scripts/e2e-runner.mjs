#!/usr/bin/env node
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  assertSupportedIsolatedCypressSpecs,
  hasExplicitCypressSpecSelection,
  selectedCypressSpecs,
} from './isolated-cypress-spec-selection.mjs'
import { startOwnedSutLifetime } from './sut-start.mjs'
import { worktreeIsolationApplies } from './browser-worktree-isolation.mjs'
import { startPrivateOpenAiMock } from './isolated-openai-mock.mjs'
import { startPrivateWikidataMock } from './isolated-wikidata-mock.mjs'
import {
  defaultSpawnCypress,
  defaultSpawnCypressOpen,
} from './e2e-cypress-process.mjs'
import { NO_CANCEL, wireBatchCancellation } from './e2e-invocation-signals.mjs'
import {
  browserFromArgv,
  resolveSpecs,
  resolveInvocationCheckout,
} from './e2e-invocation-selection.mjs'
import { runOwnedE2eInvocation } from './e2e-owned-invocation.mjs'

export { defaultSpawnCypress, defaultSpawnCypressOpen, wireBatchCancellation }
export {
  allocatePrimaryOpenAiMockPorts,
  allocatePrimaryWikidataMockPorts,
} from './e2e-primary-mock-ports.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

const DEFAULT_CYPRESS_BIN = path.join(
  repoRoot,
  'node_modules/cypress/bin/cypress'
)
const DEFAULT_CYPRESS_CONFIG_FILE = 'e2e_test/config/ci.ts'

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
    backendReload: false,
    cancelEscalationMs,
    browser,
    isolated,
    resolvedCheckoutTarget,
    ...lifetimeOpts,
  })
}

/**
 * Run one interactive Cypress session: start the owned SUT (and, when the
 * preselected spec requires it, the owned private OpenAI mock + runner
 * lease), launch Cypress in `open` mode, and keep the single stack alive
 * across every spec selection/rerun the developer makes in the Cypress UI.
 * There is NO after-spec teardown — the stack and its required mocks persist
 * until Cypress closes. When the `open` process exits, invocation cleanup
 * stops the mocks, releases the lease, and stops the SUT.
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
    backendReload: true,
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
