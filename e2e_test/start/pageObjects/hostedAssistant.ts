import { waitUntilAppIsNotBusy } from '../pageBase'

type ToolReply = { content: Array<{ text: string }> }
type AssistantStatus = {
  connected: boolean
  tokenGeneration: number
  githubVisits: string[]
  requests: Array<{ path: string; method: string }>
}

export const hostedAssistant = () => ({
  connectThroughGithub() {
    cy.task<{ authorizationUrl: string }>('assistantBegin').then(
      ({ authorizationUrl }) => {
        const authorize = new URL(authorizationUrl)
        const callback = new URL(authorize.searchParams.get('redirect_uri')!)
        expect(authorize.searchParams.get('code_challenge_method')).to.equal(
          'S256'
        )
        cy.visit(authorizationUrl)
        cy.get('a[href="/oauth2/authorization/github"]').click()
        cy.location('pathname').should('equal', '/assistant-consent')
        waitUntilAppIsNotBusy()
        cy.contains(
          'h1',
          'Allow Hosted SDK assistant to read your notebooks?'
        ).should('be.visible')
        cy.contains('Donut account: Old Learner').should('be.visible')
        cy.contains('notebooks that this account can currently read').should(
          'be.visible'
        )
        cy.contains('automatic renewal until you withdraw permission').should(
          'be.visible'
        )
        cy.contains('button', /^Allow$/).click()
        cy.origin(callback.origin, () => {
          cy.contains('h1', 'Assistant connected').should('be.visible')
        })
      }
    )
    cy.task<AssistantStatus>('assistantStatus').then((status) => {
      expect(status.connected).to.equal(true)
      expect(status.tokenGeneration).to.equal(1)
      expect(status.githubVisits).to.deep.equal([
        '/authorize',
        '/token',
        '/user',
      ])
      expect(status.requests.map((request) => request.path)).to.include.members(
        [
          '/.well-known/oauth-protected-resource/api/mcp',
          '/.well-known/oauth-authorization-server',
          '/api/oauth/token',
        ]
      )
    })
    return this
  },
  expectPrivateRead(title: string, content: string) {
    cy.task<ToolReply>('assistantCallTool', {
      name: 'list_note_candidates',
      arguments: { query: title },
    }).then((reply) => {
      const [candidate] = JSON.parse(reply.content[0]!.text)
      expect(candidate.title).to.equal(title)
      cy.task<ToolReply>('assistantCallTool', {
        name: 'read_note',
        arguments: { noteId: candidate.noteId },
      }).then((read) => {
        const note = JSON.parse(read.content[0]!.text)
        expect(note.content).to.equal(content)
        expect(note.acceptedRevision).to.match(/^[a-f0-9]{40}$/)
      })
    })
    return this
  },
})
