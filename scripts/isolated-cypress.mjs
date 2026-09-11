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
  VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK,
  startPrivateOpenAiMock,
} from './isolated-openai-mock.mjs'
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

export {
  SUPPORTED_ISOLATED_CLI_SPEC,
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
  SUPPORTED_ISOLATED_CYPRESS_SPECS,
  SUPPORTED_ISOLATED_MCP_SPEC,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
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
  const leaseToken = await acquireSutRunnerLease(checkoutRoot)

  let privateMock = null
  let cleanupOnce = async () => {
    /* replaced after lease + mock are ready */
  }

  const startMockIfNeeded = async (specs) => {
    const approved = assertSupportedIsolatedCypressSpecs(specs)
    if (!approved.requiresPrivateOpenAiMock) {
      clearOpenAiMockEndpoint(config)
      return
    }
    if (privateMock) return
    privateMock = await (
      options.startPrivateOpenAiMockFn ?? startPrivateOpenAiMock
    )({
      checkoutRoot,
      allocation,
    })
    injectOpenAiMockEndpoint(config, privateMock.endpoint)
    observePrivateMockFailure(privateMock, cleanupOnce)
  }

  cleanupOnce = registerRunnerCleanup(async () => {
    if (privateMock) {
      await privateMock.stop()
      privateMock = null
    }
    await releaseSutRunnerLease(checkoutRoot, leaseToken)
  })
  process.once('exit', () => {
    privateMock?.killSync()
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
        if (!privateMock) {
          throw new Error(
            'Isolated OpenAI mock ownership check requires a private mock for this run.'
          )
        }
        return privateMock.verifyOwnership()
      },
      [GET_ISOLATED_OPEN_AI_MOCK_ENDPOINT_TASK]() {
        return privateMock?.endpoint ?? null
      },
    })
    options.on('before:run', async (details) => {
      try {
        await startMockIfNeeded(specsFromBeforeRun(details, checkoutRoot))
        if (privateMock) {
          await privateMock.verifyOwnership()
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
    privateMock: privateMock
      ? {
          endpoint: privateMock.endpoint,
          child: privateMock.child,
          verifyOwnership: privateMock.verifyOwnership,
        }
      : null,
  }
}
