import { Mountebank } from '@anev/ts-mountebank'

export const OPEN_AI_IMPOSTER_PORT = 5001

export type RecordedImposterRequest = {
  method?: string
  path?: string
  body?: string | object
}

const openAiImposterRequestsUrl = () =>
  `${new Mountebank().mountebankUrl}/imposters/${OPEN_AI_IMPOSTER_PORT}`

export const cyFetchOpenAiImposterRequests = (): Cypress.Chainable<
  RecordedImposterRequest[]
> =>
  cy.request('GET', openAiImposterRequestsUrl()).then((res) => {
    expect(res.status).to.eq(200)
    const imposter = res.body as { requests?: RecordedImposterRequest[] }
    return imposter.requests ?? []
  })

const requestBodyAsString = (body: string | object | undefined): string =>
  typeof body === 'string' ? body : JSON.stringify(body)

export const postRequestBodies = (
  requests: RecordedImposterRequest[]
): string[] =>
  requests
    .filter((r) => r.method === 'POST')
    .map((r) => requestBodyAsString(r.body))

export const responsesPostBodies = (
  requests: RecordedImposterRequest[]
): string[] =>
  requests
    .filter(
      (r) => r.method === 'POST' && (r.path?.includes('/responses') ?? false)
    )
    .map((r) => requestBodyAsString(r.body))
