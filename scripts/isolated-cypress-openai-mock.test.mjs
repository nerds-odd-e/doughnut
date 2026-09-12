import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  guardCypressNodeSetup,
  SUPPORTED_ISOLATED_CLI_SPEC,
  SUPPORTED_ISOLATED_MCP_SPEC,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
} from './isolated-cypress.mjs'
import {
  assertSupportedIsolatedCypressSpecs,
  SUPPORTED_ISOLATED_CYPRESS_SPECS,
} from './isolated-cypress-spec-selection.mjs'
import {
  cypressArgv,
  isolatedCypressOpts,
  supportedConfig,
} from './isolated-cypress-test-helpers.mjs'
import { stubPrivateOpenAiMockHandle } from './isolated-openai-mock-test-fixtures.mjs'
import {
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  OPEN_AI_MOCK_ENDPOINT_ENV_KEY,
} from './open-ai-mock-endpoint-expose-keys.mjs'
import {
  completeIsolatedConfig,
  startLiveOwner,
} from './sut-isolated-fixtures.mjs'

test('no-mock isolated Cypress does not start a private mock process and leaves identity unchanged', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  await startLiveOwner(checkout.root, t)
  const identityBefore = readFileSync(
    path.join(checkout.root, '.worktree.local.json'),
    'utf8'
  )
  let startMockCalls = 0
  const config = supportedConfig()
  const isolated = await guardCypressNodeSetup(
    checkout.root,
    config,
    isolatedCypressOpts({
      startPrivateOpenAiMockFn: async () => {
        startMockCalls += 1
        throw new Error('private mock must not start for the no-mock spec')
      },
    })
  )
  t.after(() => isolated.release())
  assert.equal(startMockCalls, 0)
  assert.equal(isolated.privateMock, null)
  assert.equal(config.expose?.[ISOLATED_OPEN_AI_MOCK_ENV_KEY], undefined)
  assert.equal(
    readFileSync(path.join(checkout.root, '.worktree.local.json'), 'utf8'),
    identityBefore
  )
})

test('CLI and MCP isolated specs acquire no private OpenAI mock', async (t) => {
  for (const spec of [
    SUPPORTED_ISOLATED_CLI_SPEC,
    SUPPORTED_ISOLATED_MCP_SPEC,
  ]) {
    await t.test(spec, async (t) => {
      const checkout = makePrimaryCheckout(t, {
        config: JSON.stringify(completeIsolatedConfig),
      })
      await startLiveOwner(checkout.root, t)
      let startMockCalls = 0
      const isolated = await guardCypressNodeSetup(
        checkout.root,
        supportedConfig('http://localhost:5173', spec),
        isolatedCypressOpts({
          argv: cypressArgv(spec),
          startPrivateOpenAiMockFn: async () => {
            startMockCalls += 1
            throw new Error(`private mock must not start for ${spec}`)
          },
        })
      )
      t.after(() => isolated.release())
      assert.equal(startMockCalls, 0)
      assert.equal(isolated.privateMock, null)
    })
  }
})

test('OpenAI mock isolated Cypress starts a private mock after the lease and injects endpoint context', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  await startLiveOwner(checkout.root, t)
  const identityBefore = readFileSync(
    path.join(checkout.root, '.worktree.local.json'),
    'utf8'
  )
  const endpoint = {
    managementUrl: 'http://127.0.0.1:18025',
    servingPort: 18001,
  }
  let mockStarts = 0
  const config = {
    specPattern: SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
    baseUrl: 'http://localhost:5173',
  }
  const isolated = await guardCypressNodeSetup(
    checkout.root,
    config,
    isolatedCypressOpts({
      argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
      startPrivateOpenAiMockFn: async () => {
        mockStarts += 1
        return stubPrivateOpenAiMockHandle(endpoint)
      },
    })
  )
  t.after(() => isolated.release())
  assert.equal(mockStarts, 1)
  assert.deepEqual(config.expose[OPEN_AI_MOCK_ENDPOINT_ENV_KEY], endpoint)
  assert.equal(config.expose[ISOLATED_OPEN_AI_MOCK_ENV_KEY], true)
  assert.deepEqual(isolated.privateMock?.endpoint, endpoint)
  assert.equal(
    readFileSync(path.join(checkout.root, '.worktree.local.json'), 'utf8'),
    identityBefore
  )
  await isolated.release()
  const again = await guardCypressNodeSetup(
    checkout.root,
    supportedConfig(),
    isolatedCypressOpts()
  )
  t.after(() => again.release())
})

test('before:run does not start a second private mock for a confirmed selection', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  await startLiveOwner(checkout.root, t)
  const endpoint = {
    managementUrl: 'http://127.0.0.1:18025',
    servingPort: 18001,
  }
  let mockStarts = 0
  const listeners = {}
  const isolated = await guardCypressNodeSetup(
    checkout.root,
    {
      specPattern: SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
      baseUrl: 'http://localhost:5173',
    },
    isolatedCypressOpts({
      argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
      on: (event, fn) => {
        listeners[event] = fn
      },
      startPrivateOpenAiMockFn: async () => {
        mockStarts += 1
        return stubPrivateOpenAiMockHandle(endpoint)
      },
    })
  )
  t.after(() => isolated.release())
  assert.equal(mockStarts, 1)
  await listeners['before:run']({
    specs: [{ relative: SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC }],
  })
  assert.equal(mockStarts, 1)
})

test('the registry admits exactly the assessed OpenAI-mock inventory and each declares a private-mock requirement', () => {
  // The registry (`APPROVED_ISOLATED_CYPRESS_SPECS` exposed via
  // `SUPPORTED_ISOLATED_CYPRESS_SPECS`) is the single authority for which
  // specs require a private OpenAI mock — not a parallel inventory list.
  // The assessed inventory is 12 files (one representative plus the 11
  // remaining active OpenAI-mock features admitted through
  // `ACTIVE_OPEN_AI_MOCK_SPECS`); every admitted OpenAI-mock spec must
  // declare `requiresPrivateOpenAiMock: true`.
  const openAiMockSpecs = SUPPORTED_ISOLATED_CYPRESS_SPECS.filter(
    (spec) =>
      assertSupportedIsolatedCypressSpecs([spec]).requiresPrivateOpenAiMock
  )
  assert.equal(
    openAiMockSpecs.length,
    12,
    'assessed OpenAI-mock inventory is 12 files admitted by the registry'
  )
  // A mixed OpenAI-mock + application batch unions to a mock requirement.
  const mixed = assertSupportedIsolatedCypressSpecs([
    SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
    'e2e_test/features/note_creation_and_update/note_creation.feature',
  ])
  assert.equal(mixed.requiresPrivateOpenAiMock, true)
  // An application-only batch has no mock requirement.
  const appOnly = assertSupportedIsolatedCypressSpecs([
    'e2e_test/features/note_creation_and_update/note_creation.feature',
  ])
  assert.equal(appOnly.requiresPrivateOpenAiMock, false)
})

test('scenario-level mock tag is admitted via the registry requirement, not inferred from the feature filename', () => {
  // `note_view/semantic_search.feature` carries `@usingMockedOpenAiService`
  // on its scenarios, not on the Feature line. The registry declares the
  // private-mock requirement for the file; service code does not infer it
  // from the filename or tag placement.
  const scenarioLevelSpec =
    'e2e_test/features/note_view/semantic_search.feature'
  assert.equal(
    SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(scenarioLevelSpec),
    true,
    `${scenarioLevelSpec} must be admitted by the registry`
  )
  const requirement = assertSupportedIsolatedCypressSpecs([scenarioLevelSpec])
  assert.equal(
    requirement.requiresPrivateOpenAiMock,
    true,
    `${scenarioLevelSpec} must declare requiresPrivateOpenAiMock: true via the registry`
  )
  // The other two scenario-level mock-tagged files are admitted the same way.
  for (const spec of [
    'e2e_test/features/recall/property_memory_tracker.feature',
    'e2e_test/features/note_creation_and_update/mcq_management.feature',
  ]) {
    assert.equal(
      SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(spec),
      true,
      `${spec} must be admitted by the registry`
    )
    assert.equal(
      assertSupportedIsolatedCypressSpecs([spec]).requiresPrivateOpenAiMock,
      true,
      `${spec} must declare requiresPrivateOpenAiMock: true via the registry`
    )
  }
})
