import type { OpenAiMockEndpointContext } from './openAiMockEndpointContext'
import { openAiImposterRequestsUrl } from './openAiMockEndpointContext'

export type RecordedImposterRequest = {
  method?: string
  path?: string
  body?: string | object
}

const recordedRequestsFromImposterBody = (
  body: unknown
): RecordedImposterRequest[] => {
  const imposter = body as { requests?: RecordedImposterRequest[] }
  return imposter.requests ?? []
}

export const fetchOpenAiImposterRequests = async (
  endpoint: OpenAiMockEndpointContext,
  getJson: (url: string) => Promise<{ status: number; body: unknown }>
): Promise<RecordedImposterRequest[]> => {
  const res = await getJson(openAiImposterRequestsUrl(endpoint))
  if (res.status !== 200) {
    throw new Error(
      `OpenAI imposter recorded-request fetch failed: status ${res.status}`
    )
  }
  return recordedRequestsFromImposterBody(res.body)
}

export const cyFetchOpenAiImposterRequests = (
  endpoint: OpenAiMockEndpointContext
): Cypress.Chainable<RecordedImposterRequest[]> =>
  cy.request('GET', openAiImposterRequestsUrl(endpoint)).then((res) => {
    expect(res.status).to.eq(200)
    return recordedRequestsFromImposterBody(res.body)
  })

const requestBodyAsString = (body: string | object | undefined): string =>
  typeof body === 'string' ? body : JSON.stringify(body)

export const responsesPostBodies = (
  requests: RecordedImposterRequest[]
): string[] =>
  requests
    .filter(
      (r) => r.method === 'POST' && (r.path?.includes('/responses') ?? false)
    )
    .map((r) => requestBodyAsString(r.body))
