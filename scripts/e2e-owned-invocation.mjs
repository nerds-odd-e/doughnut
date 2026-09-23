import { loadCompleteIsolatedE2eAllocation } from './browser-worktree-isolation.mjs'
import { OPEN_AI_SERVICE_LABEL } from './isolated-openai-mock.mjs'
import { WIKIDATA_SERVICE_LABEL } from './isolated-wikidata-mock.mjs'
import { acquireSutRunnerLease, releaseSutRunnerLease } from './sut-owner.mjs'
import {
  E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_MOCK_PGID_ENV_KEY,
  E2E_RUNNER_OWNS_LIFETIME_ENV_KEY,
  E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY,
} from './isolated-cypress.mjs'
import { listOccupiedApplicationPorts } from './local-runtime-target.mjs'
import { isTcpPortOccupied } from './sut-healthcheck.mjs'
import { stopOwnedSutProcessTree } from './sut-owned-process-tree.mjs'
import {
  allocatePrimaryOpenAiMockPorts,
  allocatePrimaryWikidataMockPorts,
} from './e2e-primary-mock-ports.mjs'
import {
  observeChildExit,
  combineChildExits,
} from './e2e-invocation-signals.mjs'
import { runCypressOnce } from './e2e-cypress-process.mjs'

/** Own readiness, required mocks, Cypress, and cleanup for a batch or interactive session. */
export async function runOwnedE2eInvocation({
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
  const run = async () => {
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
    }
  }

  const exitCode = await run()
  let cleanupFailed = false
  const cleanup = async (resource, action) => {
    try {
      await action()
    } catch (error) {
      cleanupFailed = true
      errLog(`${label} cleanup failed (${resource}): ${error.stack ?? error}`)
    }
  }
  await cleanup('cancellation listeners', () => cancel.detach())
  for (const handle of mockHandles) {
    await cleanup('private mock', () => handle.stop())
  }
  if (leaseToken) {
    await cleanup('runner lease', () =>
      releaseSutRunnerLease(checkoutRoot, leaseToken)
    )
  }
  await cleanup('SUT lifetime', () => lifetime.shutdown())
  // The primary lifetime has no owner claim; the runner owns its process tree.
  if (!isolated) {
    await cleanup('primary SUT process tree', () =>
      stopOwnedSutProcessTree(lifetime.child)
    )
  }
  return exitCode || (cleanupFailed ? 1 : 0)
}
