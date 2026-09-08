import {
  loadCompleteIsolatedE2eAllocation,
  readPresentWorktreeLocalConfig,
  worktreeIsolationApplies,
} from './browser-worktree-isolation.mjs'
import {
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  OPEN_AI_MOCK_ENDPOINT_ENV_KEY,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
  VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK,
  startPrivateOpenAiMock,
} from './isolated-openai-mock.mjs'
import {
  assertSupportedIsolatedCypressSpecs,
  hasExplicitCypressSpecSelection,
  selectedCypressSpecs,
  specsFromBeforeRun,
} from './isolated-cypress-spec-selection.mjs'
import { isolatedBrowserOrigin } from './sut-runtime-target.mjs'
import {
  acquireSutRunnerLease,
  releaseSutRunnerLease,
  releaseSutRunnerLeaseSync,
  verifyLiveSutOwner,
} from './sut-owner.mjs'

export {
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
  SUPPORTED_ISOLATED_CYPRESS_SPECS,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
} from './isolated-cypress-spec-selection.mjs'

const PRIMARY_CYPRESS_ORIGIN = 'http://localhost:5173'

function refuseConflictingCypressOrigin(env, config, isolatedOrigin) {
  const envOrigin = env.CYPRESS_baseUrl
  if (envOrigin && envOrigin !== isolatedOrigin) {
    throw new Error(
      `Conflicting CYPRESS_baseUrl=${envOrigin} does not match the configured isolated Cypress origin (${isolatedOrigin}).`
    )
  }
  const configOrigin = config.baseUrl
  if (
    configOrigin &&
    configOrigin !== isolatedOrigin &&
    configOrigin !== PRIMARY_CYPRESS_ORIGIN
  ) {
    throw new Error(
      `Conflicting Cypress baseUrl=${configOrigin} does not match the configured isolated Cypress origin (${isolatedOrigin}).`
    )
  }
}

async function requireHealthyOwningSut(checkoutRoot, healthcheckFn) {
  const live = await verifyLiveSutOwner(checkoutRoot)
  if (!live.ok) {
    throw new Error(
      'Isolated Cypress requires a verified live SUT owner in this checkout.'
    )
  }
  const runHealth =
    healthcheckFn ??
    (async (root) => {
      const { runSutHealthcheck } = await import('./sut-healthcheck.mjs')
      return runSutHealthcheck({ checkoutRoot: root })
    })
  const health = await runHealth(checkoutRoot)
  if (!health?.ok) {
    throw new Error(
      'Isolated Cypress requires a healthy owning SUT in this checkout.'
    )
  }
}

function failLoudly(error) {
  process.stderr.write(
    `${error instanceof Error ? error.message : String(error)}\n`
  )
  process.exit(1)
}

function registerRunnerCleanup(cleanup) {
  let released = false
  const releaseOnce = async () => {
    if (released) return
    released = true
    await cleanup()
  }
  process.once('SIGINT', () => {
    releaseOnce().catch(failLoudly)
  })
  process.once('SIGTERM', () => {
    releaseOnce().catch(failLoudly)
  })
  return releaseOnce
}

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

  const observeMockFailure = (mock) => {
    mock.child?.once('exit', () => {
      const failure = mock.getFailure?.()
      if (!failure) return
      cleanupOnce()
        .catch(() => {
          /* cleanup best-effort before failLoudly */
        })
        .finally(() => failLoudly(failure))
    })
  }

  const startMockIfNeeded = async (specs) => {
    if (specs.length !== 1) {
      assertSupportedIsolatedCypressSpecs(specs)
    }
    if (specs[0] !== SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC) {
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
    observeMockFailure(privateMock)
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
    })
    options.on('before:run', async (details) => {
      try {
        const specs = specsFromBeforeRun(details, checkoutRoot)
        assertSupportedIsolatedCypressSpecs(specs)
        await startMockIfNeeded(specs)
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
