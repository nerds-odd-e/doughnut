import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  guardCypressNodeSetup,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
} from './isolated-cypress.mjs'
import {
  assertRefusesBeforeReset,
  cypressArgv,
  isolatedCypressOpts,
  supportedConfig,
} from './isolated-cypress-test-helpers.mjs'
import { startPrivateOpenAiMock } from './isolated-openai-mock.mjs'
import {
  listenForeignReadyTrackingMutations,
  spawnIdleMockChild,
  spawnOwnedManagementListener,
} from './isolated-openai-mock-test-fixtures.mjs'
import {
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  OPEN_AI_MOCK_ENDPOINT_ENV_KEY,
} from './open-ai-mock-endpoint-expose-keys.mjs'
import {
  allocateFreePort,
  closeServer,
  completeIsolatedConfig,
  isTcpListening,
  listenHttpReady,
  startLiveOwner,
} from './sut-isolated-fixtures.mjs'

test('no-mock isolated Cypress does not start a private mock process and leaves identity unchanged', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
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

test('OpenAI mock isolated Cypress starts a private mock after the lease and injects endpoint context', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  const identityBefore = readFileSync(
    path.join(checkout.root, '.worktree.local.json'),
    'utf8'
  )
  const endpoint = {
    managementUrl: 'http://127.0.0.1:18025',
    servingPort: 18001,
  }
  let mockStarts = 0
  const fakeChild = {
    once() {
      /* stub */
    },
    pid: 4242,
  }
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
        return {
          endpoint,
          child: fakeChild,
          async verifyOwnership() {
            return true
          },
          async stop() {
            /* stub */
          },
          killSync() {
            /* stub */
          },
          getFailure() {
            return null
          },
        }
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

test('foreign mock management or serving refuses Cypress setup before mutation', async (t) => {
  const ownershipCases = [
    { occupied: 'management', refusal: /refuses foreign management/ },
    { occupied: 'serving', refusal: /refuses foreign serving/ },
  ]
  for (const { occupied, refusal } of ownershipCases) {
    await t.test(`foreign ${occupied} listener`, async (t) => {
      const checkout = makePrimaryCheckout(t, {
        config: JSON.stringify(completeIsolatedConfig),
      })
      const live = await startLiveOwner(checkout.root)
      t.after(() => live.server.close())

      const foreign = await listenForeignReadyTrackingMutations()
      t.after(() => closeServer(foreign.server))
      const peerBody = 'peer-mock-response'
      const peer = await listenHttpReady(peerBody)
      t.after(() => closeServer(peer.server))

      const ports =
        occupied === 'management'
          ? {
              managementPort: foreign.port,
              servingPort: await allocateFreePort(),
            }
          : {
              managementPort: await allocateFreePort(),
              servingPort: foreign.port,
            }

      const config = {
        specPattern: SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
        baseUrl: 'http://localhost:5173',
      }
      await assertRefusesBeforeReset(
        () =>
          guardCypressNodeSetup(
            checkout.root,
            config,
            isolatedCypressOpts({
              argv: cypressArgv(SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC),
              startPrivateOpenAiMockFn: (opts) =>
                startPrivateOpenAiMock({
                  ...opts,
                  allocatePortsFn: async () => ports,
                  spawnFn: () =>
                    occupied === 'management'
                      ? spawnIdleMockChild()
                      : spawnOwnedManagementListener(ports.managementPort),
                }),
            })
          ),
        refusal
      )

      assert.equal(foreign.mutationCount(), 0)
      assert.equal(config.expose?.[OPEN_AI_MOCK_ENDPOINT_ENV_KEY], undefined)
      assert.equal(await isTcpListening(foreign.port), true)
      assert.equal(
        await (await fetch(`http://127.0.0.1:${peer.port}/`)).text(),
        peerBody
      )
    })
  }
})
