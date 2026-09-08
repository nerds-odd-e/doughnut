/**
 * Spawned runner boundary for the owned-mock-failure proof.
 * Starts isolated Cypress setup with an idle private mock, writes ready.json,
 * then stays alive until observePrivateMockFailure fails the process.
 */
import { writeFileSync } from 'node:fs'
import { guardCypressNodeSetup } from './isolated-cypress.mjs'
import { SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC } from './isolated-openai-mock.mjs'
import { spawnIdlePrivateMockHandle } from './isolated-openai-mock-test-fixtures.mjs'

const checkoutRoot = process.env.CHECKOUT_ROOT
const readyFile = process.env.READY_FILE
const spec = SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC

const isolated = await guardCypressNodeSetup(
  checkoutRoot,
  { specPattern: spec, baseUrl: 'http://localhost:5173' },
  {
    argv: ['node', 'cypress', 'run', '--spec', spec],
    env: process.env,
    healthcheckFn: async () => ({ ok: true }),
    startPrivateOpenAiMockFn: async () => spawnIdlePrivateMockHandle(),
  }
)

writeFileSync(
  readyFile,
  JSON.stringify({ mockPid: isolated.privateMock.child.pid })
)
setInterval(() => {
  /* keep runner alive until the test kills the mock */
}, 1000)
