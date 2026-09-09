import { openAiImposterRequestsUrl } from './openAiMockEndpointContext'
import type { OpenAiMockEndpointContext } from './openAiMockEndpointContext'

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

export const recordedResponsesPostsMatchMarkers = (
  requests: RecordedImposterRequest[],
  requiredMarker: string,
  forbiddenMarker?: string
): boolean => {
  const joinedBodies = responsesPostBodies(requests).join('\n')
  return (
    joinedBodies.includes(requiredMarker) &&
    (forbiddenMarker === undefined || !joinedBodies.includes(forbiddenMarker))
  )
}
