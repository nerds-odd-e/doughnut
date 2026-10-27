export type ToolReply = { content: Array<{ text: string }> }
export type AssistantStatus = {
  connected: boolean
  tokenGeneration: number
  githubVisits: string[]
  requests: Array<{
    path: string
    method: string
    status: number
    generation: number
    grantType?: string
    resource?: string
  }>
  replacements: Array<{
    generation: number
    accessChanged: boolean
    refreshChanged: boolean
    issuer: string
    scope: string
    resource: string
    tokenEndpointOrigin: string
  }>
}

export function expectRenewedAssistant(
  before: AssistantStatus,
  after: AssistantStatus,
  origin: string
) {
  expect(after.githubVisits).to.deep.equal(before.githubVisits)
  expect(after.tokenGeneration, JSON.stringify({ before, after })).to.equal(
    before.tokenGeneration + 1
  )
  const replacement = after.replacements.at(-1)!
  expect(replacement).to.include({
    accessChanged: true,
    refreshChanged: true,
    scope: 'notes:read',
  })
  expect(origin).to.match(/^https:\/\//)
  expect(replacement).to.include({
    issuer: origin,
    tokenEndpointOrigin: origin,
    resource: `${origin}/api/mcp`,
  })
  expect(replacement.issuer).to.equal(before.replacements[0]!.issuer)
  expect(replacement.resource).to.equal(before.replacements[0]!.resource)
  const calls = after.requests.slice(before.requests.length)
  expect(
    calls
      .filter((call) => call.path === '/api/mcp')
      .map((call) => [call.status, call.generation])
  ).to.deep.equal([
    [401, before.tokenGeneration],
    [200, after.tokenGeneration],
  ])
  expect(calls.find((call) => call.grantType === 'refresh_token')).to.include({
    status: 200,
    resource: replacement.resource,
  })
}
