import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
  SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC,
} from './isolated-cypress-spec-selection.mjs'
import { spawnIdlePrivateMockHandle } from './isolated-openai-mock-test-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import {
  E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY,
  E2E_RUNNER_OWNS_LIFETIME_ENV_KEY,
  E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
} from './isolated-cypress.mjs'
import { runE2eInteractive } from './e2e-runner.mjs'
import {
  cypressArgv,
  makeOpenCypressChild,
} from './e2e-runner-cypress-fixtures.mjs'
import {
  isolatedLifetimeOpts,
  healthcheckWaitingForPids,
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

test('interactive session: bounded two-service set provisioned once covers switches between ordinary, OpenAI and Wikidata features; reruns preserve ownership; close cleans all owned resources', async (t) => {
  // Slice 8: one interactive session switches between ordinary, OpenAI and
  // Wikidata features with the required private endpoints ready BEFORE use.
  // Interactive choices are not known at launch, so provisioning the bounded
  // two-service set (OpenAI + Wikidata) once for the session is sufficient.
  // The preselected batch unions both mock requirements; the wrapper owns
  // both mocks + a single lease for the whole session. No background resource
  // broker; no Cucumber scenario semantics change; the existing
  // preselection/session protocol is reused.
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let openAiStarts = 0
  let wikidataStarts = 0
  let openAiStopCalls = 0
  let wikidataStopCalls = 0
  let openAiPid = 0
  let wikidataPid = 0
  let openAiEndpoint = null
  let wikidataEndpoint = null
  let capturedEnv = null

  const code = await runE2eInteractive({
    argv: cypressArgv(
      `${SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC},${SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC}`
    ),
    ...isolatedLifetimeOpts(checkout.root, standIn, {
      healthcheckFn: healthcheckWaitingForPids(standIn.pidsFile),
    }),
    startPrivateOpenAiMockFn: async () => {
      openAiStarts += 1
      const handle = spawnIdlePrivateMockHandle({
        managementUrl: 'http://127.0.0.1:18025',
        servingPort: 18001,
      })
      openAiPid = handle.child.pid
      openAiEndpoint = handle.endpoint
      const realStop = handle.stop
      handle.stop = async () => {
        openAiStopCalls += 1
        await realStop()
      }
      return handle
    },
    startPrivateWikidataMockFn: async () => {
      wikidataStarts += 1
      const handle = spawnIdlePrivateMockHandle({
        managementUrl: 'http://127.0.0.1:18026',
        servingPort: 18002,
      })
      wikidataPid = handle.child.pid
      wikidataEndpoint = handle.endpoint
      const realStop = handle.stop
      handle.stop = async () => {
        wikidataStopCalls += 1
        await realStop()
      }
      return handle
    },
    spawnCypress: (opts) => {
      capturedEnv = opts.env
      return makeOpenCypressChild(async (session) => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
        // Resource selection: both private endpoints are ready BEFORE the
        // browser launches (injected into Cypress env under distinct keys).
        assert.equal(
          capturedEnv[E2E_RUNNER_OWNS_LIFETIME_ENV_KEY],
          '1',
          'plugin must be in adapter mode (wrapper owns the session lifetime)'
        )
        const injectedOpenAi = JSON.parse(
          capturedEnv[E2E_RUNNER_MOCK_ENDPOINT_ENV_KEY]
        )
        const injectedWikidata = JSON.parse(
          capturedEnv[E2E_RUNNER_WIKIDATA_MOCK_ENDPOINT_ENV_KEY]
        )
        assert.deepEqual(injectedOpenAi, openAiEndpoint)
        assert.deepEqual(injectedWikidata, wikidataEndpoint)
        assert.notDeepEqual(injectedOpenAi, injectedWikidata)

        // Across the session — selecting the ordinary feature, switching to
        // the OpenAI feature, switching to the Wikidata feature, and rerunning
        // — the SAME stack + both mocks stay alive: the bounded two-service set
        // is provisioned once and no after-spec teardown happens between
        // selections/reruns.
        assert.equal(isPidAlive(state.owned.leader), true)
        assert.equal(isPidAlive(openAiPid), true)
        assert.equal(isPidAlive(wikidataPid), true)
        assert.equal(openAiStarts, 1, 'OpenAI mock must start exactly once')
        assert.equal(wikidataStarts, 1, 'Wikidata mock must start exactly once')
        assert.equal(
          openAiStopCalls,
          0,
          'no after-spec teardown of OpenAI mock'
        )
        assert.equal(
          wikidataStopCalls,
          0,
          'no after-spec teardown of Wikidata mock'
        )

        // Closing Cypress ends the session.
        session.close()
      })
    },
  })

  assert.equal(code, 0, 'a clean close must return 0')
  // Both mocks were started exactly once for the whole session.
  assert.equal(
    openAiStarts,
    1,
    'OpenAI mock must start exactly once for the session'
  )
  assert.equal(
    wikidataStarts,
    1,
    'Wikidata mock must start exactly once for the session'
  )
  // Close cleans all owned resources: both mocks stopped once.
  assert.equal(openAiStopCalls, 1, 'OpenAI mock must be stopped once on close')
  assert.equal(
    wikidataStopCalls,
    1,
    'Wikidata mock must be stopped once on close'
  )
  // Zero surviving owned processes (both mocks + SUT leader + grandchild).
  assert.equal(isPidAlive(openAiPid), false)
  assert.equal(isPidAlive(wikidataPid), false)
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  // Ownership released.
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})
