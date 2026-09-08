/** Mountebank management + OpenAI imposter serving endpoints for one mock run. */
import {
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  OPEN_AI_MOCK_ENDPOINT_ENV_KEY,
} from '../../../scripts/open-ai-mock-endpoint-expose-keys.mjs'

export type OpenAiMockEndpointContext = {
  managementUrl: string
  servingPort: number
}

/** Shared primary/CI defaults — selected only at this OpenAI mock boundary. */
export const SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT: OpenAiMockEndpointContext = {
  managementUrl: 'http://localhost:2525',
  servingPort: 5001,
}

export {
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  OPEN_AI_MOCK_ENDPOINT_ENV_KEY,
  VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK,
} from '../../../scripts/open-ai-mock-endpoint-expose-keys.mjs'

export const openAiImposterRequestsUrl = (
  endpoint: OpenAiMockEndpointContext
): string => `${endpoint.managementUrl}/imposters/${endpoint.servingPort}`

function isCompleteEndpoint(
  value: unknown
): value is OpenAiMockEndpointContext {
  if (!value || typeof value !== 'object') return false
  const candidate = value as OpenAiMockEndpointContext
  return (
    typeof candidate.managementUrl === 'string' &&
    candidate.managementUrl.length > 0 &&
    Number.isInteger(candidate.servingPort) &&
    candidate.servingPort > 0
  )
}

/**
 * Isolated mock runs require a complete injected context (never 2525/5001
 * fallback). Primary/CI keep shared defaults when no context is injected.
 */
export function resolveOpenAiMockEndpointContext(
  readEnv: (key: string) => unknown = (key) =>
    typeof Cypress !== 'undefined' ? Cypress.expose(key) : undefined
): OpenAiMockEndpointContext {
  const isolated = readEnv(ISOLATED_OPEN_AI_MOCK_ENV_KEY)
  const fromEnv = readEnv(OPEN_AI_MOCK_ENDPOINT_ENV_KEY)
  if (isolated) {
    if (!isCompleteEndpoint(fromEnv)) {
      throw new Error(
        'Isolated OpenAI mock requires a complete OPEN_AI_MOCK_ENDPOINT_CONTEXT; ' +
          'refusing shared 2525/5001 fallback.'
      )
    }
    return {
      managementUrl: fromEnv.managementUrl,
      servingPort: fromEnv.servingPort,
    }
  }
  if (fromEnv == null) {
    return SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT
  }
  if (!isCompleteEndpoint(fromEnv)) {
    throw new Error(
      'OPEN_AI_MOCK_ENDPOINT_CONTEXT must include managementUrl and servingPort.'
    )
  }
  return {
    managementUrl: fromEnv.managementUrl,
    servingPort: fromEnv.servingPort,
  }
}
