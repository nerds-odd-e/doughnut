/** Mountebank management + Wikidata imposter serving endpoint for one mock run. */
import {
  GET_ISOLATED_WIKIDATA_MOCK_ENDPOINT_TASK,
  ISOLATED_WIKIDATA_MOCK_ENV_KEY,
  WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
} from '../../../scripts/wikidata-mock-endpoint-expose-keys.mjs'

export type WikidataMockEndpointContext = {
  managementUrl: string
  servingPort: number
}

/** Shared primary/CI defaults — selected only at this Wikidata mock boundary. */
export const SHARED_WIKIDATA_MOCK_ENDPOINT_CONTEXT: WikidataMockEndpointContext =
  {
    managementUrl: 'http://localhost:2525',
    servingPort: 5002,
  }

export {
  GET_ISOLATED_WIKIDATA_MOCK_ENDPOINT_TASK,
  ISOLATED_WIKIDATA_MOCK_ENV_KEY,
  VERIFY_ISOLATED_WIKIDATA_MOCK_OWNERSHIP_TASK,
  WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
} from '../../../scripts/wikidata-mock-endpoint-expose-keys.mjs'

export const wikidataImposterRequestUrl = (
  endpoint: WikidataMockEndpointContext
): string => `${endpoint.managementUrl}/imposters/${endpoint.servingPort}`

function isCompleteEndpoint(
  value: unknown
): value is WikidataMockEndpointContext {
  if (!value || typeof value !== 'object') return false
  const candidate = value as WikidataMockEndpointContext
  return (
    typeof candidate.managementUrl === 'string' &&
    candidate.managementUrl.length > 0 &&
    Number.isInteger(candidate.servingPort) &&
    candidate.servingPort > 0
  )
}

/**
 * Cypress 16 `expose` is fixed after setupNodeEvents. Private mocks often start
 * in `before:run`, so the browser applies the task-fetched override instead.
 */
let browserEndpointOverride: WikidataMockEndpointContext | null = null

export function setBrowserWikidataMockEndpointOverride(
  endpoint: WikidataMockEndpointContext | null
) {
  browserEndpointOverride = endpoint
}

export function clearBrowserWikidataMockEndpointOverride() {
  browserEndpointOverride = null
}

/**
 * Isolated mock runs require a complete injected context (never 2525/5002
 * fallback). Primary/CI keep shared defaults when no context is injected.
 */
export function resolveWikidataMockEndpointContext(
  readEnv: (key: string) => unknown = (key) =>
    typeof Cypress !== 'undefined' ? Cypress.expose(key) : undefined
): WikidataMockEndpointContext {
  if (browserEndpointOverride) {
    return {
      managementUrl: browserEndpointOverride.managementUrl,
      servingPort: browserEndpointOverride.servingPort,
    }
  }
  const isolated = readEnv(ISOLATED_WIKIDATA_MOCK_ENV_KEY)
  const fromEnv = readEnv(WIKIDATA_MOCK_ENDPOINT_ENV_KEY)
  if (isolated) {
    if (!isCompleteEndpoint(fromEnv)) {
      throw new Error(
        'Isolated Wikidata mock requires a complete WIKIDATA_MOCK_ENDPOINT_CONTEXT; ' +
          'refusing shared 2525/5002 fallback.'
      )
    }
    return {
      managementUrl: fromEnv.managementUrl,
      servingPort: fromEnv.servingPort,
    }
  }
  if (fromEnv == null) {
    return SHARED_WIKIDATA_MOCK_ENDPOINT_CONTEXT
  }
  if (!isCompleteEndpoint(fromEnv)) {
    throw new Error(
      'WIKIDATA_MOCK_ENDPOINT_CONTEXT must include managementUrl and servingPort.'
    )
  }
  return {
    managementUrl: fromEnv.managementUrl,
    servingPort: fromEnv.servingPort,
  }
}

export function loadBrowserWikidataMockEndpointOverrideFromTask() {
  return cy
    .task<WikidataMockEndpointContext | null>(
      GET_ISOLATED_WIKIDATA_MOCK_ENDPOINT_TASK
    )
    .then((endpoint) => {
      if (endpoint) {
        if (!isCompleteEndpoint(endpoint)) {
          throw new Error(
            'Isolated Wikidata mock task returned an incomplete endpoint.'
          )
        }
        setBrowserWikidataMockEndpointOverride({
          managementUrl: endpoint.managementUrl,
          servingPort: endpoint.servingPort,
        })
      } else {
        clearBrowserWikidataMockEndpointOverride()
      }
      return endpoint
    })
}
