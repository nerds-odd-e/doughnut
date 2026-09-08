/** Mountebank management + OpenAI imposter serving endpoints for one mock run. */
export type OpenAiMockEndpointContext = {
  managementUrl: string
  servingPort: number
}

/** Shared primary/CI defaults — selected only at this OpenAI mock boundary. */
export const SHARED_OPEN_AI_MOCK_ENDPOINT_CONTEXT: OpenAiMockEndpointContext = {
  managementUrl: 'http://localhost:2525',
  servingPort: 5001,
}

export const openAiImposterRequestsUrl = (
  endpoint: OpenAiMockEndpointContext
): string => `${endpoint.managementUrl}/imposters/${endpoint.servingPort}`
