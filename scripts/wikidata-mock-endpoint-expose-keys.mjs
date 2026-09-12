/** Cypress `expose` keys and node task for private Wikidata mock endpoint routing. */

export const WIKIDATA_MOCK_ENDPOINT_ENV_KEY = 'WIKIDATA_MOCK_ENDPOINT_CONTEXT'
export const ISOLATED_WIKIDATA_MOCK_ENV_KEY = 'ISOLATED_WIKIDATA_MOCK'
export const VERIFY_ISOLATED_WIKIDATA_MOCK_OWNERSHIP_TASK =
  'verifyIsolatedWikidataMockOwnership'
/** Browser reads the before:run private mock via this task (expose is setup-only). */
export const GET_ISOLATED_WIKIDATA_MOCK_ENDPOINT_TASK =
  'getIsolatedWikidataMockEndpoint'
