/** Cypress `expose` keys and node task for private OpenAI mock endpoint routing. */

export const OPEN_AI_MOCK_ENDPOINT_ENV_KEY = 'OPEN_AI_MOCK_ENDPOINT_CONTEXT'
export const ISOLATED_OPEN_AI_MOCK_ENV_KEY = 'ISOLATED_OPEN_AI_MOCK'
export const VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK =
  'verifyIsolatedOpenAiMockOwnership'
/** Browser reads the before:run private mock via this task (expose is setup-only). */
export const GET_ISOLATED_OPEN_AI_MOCK_ENDPOINT_TASK =
  'getIsolatedOpenAiMockEndpoint'
