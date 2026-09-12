import {
  loadCompleteIsolatedE2eAllocation,
  readPresentWorktreeLocalConfig,
  worktreeIsolationApplies,
} from './browser-worktree-isolation.mjs'
import { WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY } from './worktree-isolation-constants.mjs'
import {
  GET_ISOLATED_OPEN_AI_MOCK_ENDPOINT_TASK,
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  OPEN_AI_MOCK_ENDPOINT_ENV_KEY,
  OPEN_AI_SERVICE_LABEL,
  VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK,
  startPrivateOpenAiMock,
} from './isolated-openai-mock.mjs'
import {
  GET_ISOLATED_WIKIDATA_MOCK_ENDPOINT_TASK,
  ISOLATED_WIKIDATA_MOCK_ENV_KEY,
  VERIFY_ISOLATED_WIKIDATA_MOCK_OWNERSHIP_TASK,
  WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
  WIKIDATA_SERVICE_LABEL,
  startPrivateWikidataMock,
} from './isolated-wikidata-mock.mjs'
import { assertOwnedMockListener } from './isolated-mountebank-mock-ownership.mjs'
import {
  assertSupportedIsolatedCypressSpecs,
  hasExplicitCypressSpecSelection,
  selectedCypressSpecs,
  specsFromBeforeRun,
} from './isolated-cypress-spec-selection.mjs'
import {
  observePrivateMockFailure,
  refuseConflictingCypressOrigin,
  registerRunnerCleanup,
  requireHealthyOwningSut,
} from './isolated-cypress-runner-lifecycle.mjs'
import { isolatedBrowserOrigin } from './sut-runtime-target.mjs'
import {
  acquireSutRunnerLease,
  releaseSutRunnerLease,
  releaseSutRunnerLeaseSync,
} from './sut-owner.mjs'

/**
 * When the invocation wrapper (runE2eBatch) owns the private mock + runner
 * lease for the whole batch, it sets this env var so the Cypress plugin acts
 * only as an adapter: inject the owned endpoint into `expose` and register
 * the read-only ownership/endpoint tasks. The plugin does NOT start/stop the
 * mock or acquire/release the lease in adapter mode.
 */
export const E2E_RUNNER_OWNS_LIFETIME_ENV_KEY = 'E2E_RUNNER_OWNS_LIFETIME'
export const E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY = 'E2E_RUNNER_MOCK_ENDPOINT'
export const E2E_RUNNER_MOCK_PGID_ENV_KEY = 'E2E_RUNNER_MOCK_PGID'
export const E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY =
  'E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT'
export const E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY =
  'E2E_RUNNER_WIKIDATA_MOCK_PGID'

export {
  SUPPORTED_ISOLATED_CLI_SPEC,
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
  SUPPORTED_ISOLATED_CYPRESS_SPECS,
  SUPPORTED_ISOLATED_MCP_SPEC,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
  SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC,
} from './isolated-cypress-spec-selection.mjs'

function injectOpenAiMockEndpoint(config, endpoint) {
  if (!config.expose || typeof config.expose !== 'object') {
    config.expose = {}
  }
  config.expose[OPEN_AI_MOCK_ENDPOINT_ENV_KEY] = endpoint
  config.expose[ISOLATED_OPEN_AI_MOCK_ENV_KEY] = true
}

function clearOpenAiMockEndpoint(config) {
  if (!config.expose || typeof config.expose !== 'object') return
  delete config.expose[OPEN_AI_MOCK_ENDPOINT_ENV_KEY]
  delete config.expose[ISOLATED_OPEN_AI_MOCK_ENV_KEY]
}

function injectWikidataMockEndpoint(config, endpoint) {
  if (!config.expose || typeof config.expose !== 'object') {
    config.expose = {}
  }
  config.expose[WIKIDATA_MOCK_ENDPOINT_ENV_KEY] = endpoint
  config.expose[ISOLATED_WIKIDATA_MOCK_ENV_KEY] = true
}

function clearWikidataMockEndpoint(config) {
  if (!config.expose || typeof config.expose !== 'object') return
  delete config.expose[WIKIDATA_MOCK_ENDPOINT_ENV_KEY]
  delete config.expose[ISOLATED_WIKIDATA_MOCK_ENV_KEY]
}

function parseRunnerOwnedEndpoint(env) {
  const raw = env[E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY]
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    if (
      parsed &&
      typeof parsed.managementUrl === 'string' &&
      Number.isInteger(parsed.servingPort) &&
      parsed.servingPort > 0
    ) {
      return parsed
    }
  } catch {
    // malformed wrapper endpoint
  }
  return null
}

function parseRunnerOwnedMockPgid(env) {
  const raw = env[E2E_RUNNER_MOCK_PGID_ENV_KEY]
  const pid = Number(raw)
  return Number.isInteger(pid) && pid > 0 ? pid : null
}

function parseRunnerOwnedWikidataEndpoint(env) {
  const raw = env[E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY]
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    if (
      parsed &&
      typeof parsed.managementUrl === 'string' &&
      Number.isInteger(parsed.servingPort) &&
      parsed.servingPort > 0
    ) {
      return parsed
    }
  } catch {
    // malformed wrapper endpoint
  }
  return null
}

function parseRunnerOwnedWikidataMockPgid(env) {
  const raw = env[E2E_RUNNER_WIKIDATA_MOCK_PGID_ENV_KEY]
  const pid = Number(raw)
  return Number.isInteger(pid) && pid > 0 ? pid : null
}

function verifyOwnedMockListenerTasks(endpoint, mockPgid, serviceLabel) {
  return async () => {
    if (!endpoint) {
      throw new Error(
        `Isolated ${serviceLabel} mock ownership check requires a private mock for this run.`
      )
    }
    if (!mockPgid) {
      throw new Error(
        `Isolated ${serviceLabel} mock ownership check requires the owned mock process group id.`
      )
    }
    await assertOwnedMockListener(endpoint.servingPort, mockPgid, 'serving', {
      serviceLabel,
    })
    await assertOwnedMockListener(
      new URL(endpoint.managementUrl).port,
      mockPgid,
      'management',
      { serviceLabel }
    )
    return true
  }
}

/**
 * Adapter mode: the invocation wrapper (runE2eBatch) already owns the private
 * mock + runner lease for the whole batch. The plugin only injects the owned
 * endpoint into `expose` (so the browser sees it before configuration is
 * consumed) and registers the read-only endpoint/ownership tasks. It does NOT
 * start/stop the mock, acquire/release the lease, or register after:spec /
 * after:run cleanup — the wrapper's single lifetime owns all of that.
 */
async function adapterCypressNodeSetup(checkoutRoot, config, options, env) {
  const endpoint = parseRunnerOwnedEndpoint(env)
  const mockPgid = parseRunnerOwnedMockPgid(env)
  const wikidataEndpoint = parseRunnerOwnedWikidataEndpoint(env)
  const wikidataMockPgid = parseRunnerOwnedWikidataMockPgid(env)
  if (endpoint) {
    injectOpenAiMockEndpoint(config, endpoint)
  }
  if (wikidataEndpoint) {
    injectWikidataMockEndpoint(config, wikidataEndpoint)
  }
  if (typeof options.on === 'function') {
    options.on('task', {
      async [VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK]() {
        return verifyOwnedMockListenerTasks(
          endpoint,
          mockPgid,
          OPEN_AI_SERVICE_LABEL
        )()
      },
      [GET_ISOLATED_OPEN_AI_MOCK_ENDPOINT_TASK]() {
        return endpoint ?? null
      },
      async [VERIFY_ISOLATED_WIKIDATA_MOCK_OWNERSHIP_TASK]() {
        return verifyOwnedMockListenerTasks(
          wikidataEndpoint,
          wikidataMockPgid,
          WIKIDATA_SERVICE_LABEL
        )()
      },
      [GET_ISOLATED_WIKIDATA_MOCK_ENDPOINT_TASK]() {
        return wikidataEndpoint ?? null
      },
    })
  }
  return {
    release: async () => {
      /* wrapper owns the lifetime; the plugin has nothing to release */
    },
    origin: null,
    privateMock: endpoint
      ? { endpoint, child: null, verifyOwnership: async () => true }
      : null,
  }
}

export async function guardCypressNodeSetup(
  checkoutRoot,
  config = {},
  options = {}
) {
  readPresentWorktreeLocalConfig(checkoutRoot)
  if (!worktreeIsolationApplies(checkoutRoot)) {
    return
  }
  if (!config.expose || typeof config.expose !== 'object') {
    config.expose = {}
  }
  config.expose[WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY] = true
  const argv = options.argv ?? process.argv
  const env = options.env ?? process.env
  const explicitSpecs = hasExplicitCypressSpecSelection(
    argv,
    config.specPattern
  )
  const selectedWhenKnown = explicitSpecs
    ? selectedCypressSpecs({
        argv,
        specPattern: config.specPattern,
        checkoutRoot,
      })
    : null
  if (explicitSpecs) {
    assertSupportedIsolatedCypressSpecs(selectedWhenKnown)
  } else if (typeof options.on !== 'function') {
    assertSupportedIsolatedCypressSpecs(
      selectedCypressSpecs({
        argv,
        specPattern: config.specPattern,
        checkoutRoot,
      })
    )
  }
  const allocation = loadCompleteIsolatedE2eAllocation(checkoutRoot)
  const origin = isolatedBrowserOrigin(allocation.e2e)
  refuseConflictingCypressOrigin(env, config, origin)
  await requireHealthyOwningSut(checkoutRoot, options.healthcheckFn)

  // Adapter mode: the invocation wrapper owns the mock + lease for the batch.
  if (env[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY] === '1') {
    config.baseUrl = origin
    const adapted = await adapterCypressNodeSetup(
      checkoutRoot,
      config,
      options,
      env
    )
    return { ...adapted, origin }
  }

  const leaseToken = await acquireSutRunnerLease(checkoutRoot)

  const privateMocks = {}
  let cleanupOnce = async () => {
    /* replaced after lease + mock are ready */
  }

  const startMockIfNeeded = async (specs) => {
    const approved = assertSupportedIsolatedCypressSpecs(specs)
    if (approved.requiresPrivateOpenAiMock && !privateMocks.openAi) {
      privateMocks.openAi = await (
        options.startPrivateOpenAiMockFn ?? startPrivateOpenAiMock
      )({
        checkoutRoot,
        allocation,
      })
      injectOpenAiMockEndpoint(config, privateMocks.openAi.endpoint)
      observePrivateMockFailure(privateMocks.openAi, cleanupOnce)
    }
    if (approved.requiresPrivateWikidataMock && !privateMocks.wikidata) {
      privateMocks.wikidata = await (
        options.startPrivateWikidataMockFn ?? startPrivateWikidataMock
      )({
        checkoutRoot,
        allocation,
      })
      injectWikidataMockEndpoint(config, privateMocks.wikidata.endpoint)
      observePrivateMockFailure(privateMocks.wikidata, cleanupOnce)
    }
    if (
      !(
        approved.requiresPrivateOpenAiMock ||
        approved.requiresPrivateWikidataMock
      )
    ) {
      clearOpenAiMockEndpoint(config)
      clearWikidataMockEndpoint(config)
    }
  }

  cleanupOnce = registerRunnerCleanup(async () => {
    for (const mock of Object.values(privateMocks)) {
      try {
        await mock.stop()
      } catch {
        // best-effort during cleanup
      }
    }
    for (const key of Object.keys(privateMocks)) {
      delete privateMocks[key]
    }
    await releaseSutRunnerLease(checkoutRoot, leaseToken)
  })
  process.once('exit', () => {
    for (const mock of Object.values(privateMocks)) {
      mock?.killSync()
    }
    releaseSutRunnerLeaseSync(checkoutRoot, leaseToken)
  })

  try {
    if (selectedWhenKnown) {
      await startMockIfNeeded(selectedWhenKnown)
    }
  } catch (error) {
    await cleanupOnce()
    throw error
  }

  if (typeof options.on === 'function') {
    options.on('task', {
      async [VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK]() {
        if (!privateMocks.openAi) {
          throw new Error(
            'Isolated OpenAI mock ownership check requires a private mock for this run.'
          )
        }
        return privateMocks.openAi.verifyOwnership()
      },
      [GET_ISOLATED_OPEN_AI_MOCK_ENDPOINT_TASK]() {
        return privateMocks.openAi?.endpoint ?? null
      },
      async [VERIFY_ISOLATED_WIKIDATA_MOCK_OWNERSHIP_TASK]() {
        if (!privateMocks.wikidata) {
          throw new Error(
            'Isolated Wikidata mock ownership check requires a private mock for this run.'
          )
        }
        return privateMocks.wikidata.verifyOwnership()
      },
      [GET_ISOLATED_WIKIDATA_MOCK_ENDPOINT_TASK]() {
        return privateMocks.wikidata?.endpoint ?? null
      },
    })
    options.on('before:run', async (details) => {
      try {
        await startMockIfNeeded(specsFromBeforeRun(details, checkoutRoot))
        for (const mock of Object.values(privateMocks)) {
          await mock.verifyOwnership()
        }
      } catch (error) {
        await cleanupOnce()
        throw error
      }
    })
    options.on('after:spec', cleanupOnce)
    options.on('after:run', cleanupOnce)
  }

  config.baseUrl = origin
  return {
    release: cleanupOnce,
    origin,
    privateMock: privateMocks.openAi
      ? {
          endpoint: privateMocks.openAi.endpoint,
          child: privateMocks.openAi.child,
          verifyOwnership: privateMocks.openAi.verifyOwnership,
        }
      : null,
  }
}
