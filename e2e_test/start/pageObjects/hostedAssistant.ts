import { waitUntilAppIsNotBusy } from '../pageBase'
import router from '../router'

import {
  expectRenewedAssistant,
  type ToolReply,
  type AssistantStatus,
} from './hostedAssistantStatus'

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
        this.expectInformedConsent()
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
      expect(candidate.notebookName).to.equal('SDK Notebook')
      cy.task<ToolReply>('assistantCallTool', {
        name: 'read_note',
        arguments: { noteId: candidate.noteId },
      }).then((read) => {
        const note = JSON.parse(read.content[0]!.text)
        expect(note.content).to.equal(content)
        expect(note.notebookName).to.equal('SDK Notebook')
        expect(note.acceptedRevision).to.match(/^[a-f0-9]{40}$/)
        cy.wrap(note).as('hostedNote')
      })
    })
    return this
  },
  expectBoundedGraph(title: string) {
    cy.get<{ noteId: number }>('@hostedNote').then(({ noteId }) => {
      cy.task<ToolReply>('assistantCallTool', {
        name: 'get_note_graph',
        arguments: { noteId, tokenLimit: 1000 },
      }).then((reply) => {
        const graph = JSON.parse(reply.content[0]!.text)
        expect(graph.focusNote.title).to.equal(title)
        expect(JSON.stringify(graph)).to.contain('Hosted SDK Sibling')
      })
      cy.task<ToolReply>('assistantCallTool', {
        name: 'get_note_graph',
        arguments: { noteId, tokenLimit: 10 },
      }).then((reply) => {
        expect(JSON.stringify(reply)).to.contain(title)
        expect(JSON.stringify(reply)).not.to.contain('Hosted SDK Sibling')
      })
    })
    return this
  },
  waitForAccessExpiry() {
    cy.task<AssistantStatus>('assistantStatus').as('beforeRenewal')
    cy.task<{ expired: boolean }>('assistantWaitForAccessExpiry')
      .its('expired')
      .should('equal', true)
    return this
  },
  expectAutomaticRenewal() {
    cy.get<Record<string, unknown>>('@hostedNote').then((note) => {
      cy.task<ToolReply>('assistantCallTool', {
        name: 'read_note',
        arguments: { noteId: note.noteId },
      }).then((reply) =>
        expect(JSON.parse(reply.content[0]!.text)).to.deep.equal(note)
      )
    })
    cy.get<AssistantStatus>('@beforeRenewal').then((before) => {
      cy.task<AssistantStatus>('assistantStatus').then((after) => {
        expectRenewedAssistant(before, after, Cypress.config('baseUrl')!)
      })
    })
    return this
  },
  withdrawInSettings() {
    router().visitNamed('settingsConnectedAssistants')
    waitUntilAppIsNotBusy()
    cy.findByRole('button', {
      name: 'Withdraw access for Hosted SDK assistant',
    }).click()
    waitUntilAppIsNotBusy()
    cy.contains('No connected assistants.').should('be.visible')
    return this
  },
  expectWithdrawal() {
    cy.task<AssistantStatus>('assistantStatus').then((before) => {
      cy.task('assistantWithdrawnTool', {
        name: 'list_note_candidates',
        arguments: { query: 'Hosted SDK Marker' },
      }).should('deep.equal', { authorizationRequired: true })
      cy.task<AssistantStatus>('assistantStatus').then((after) => {
        const calls = after.requests.slice(before.requests.length)
        expect(calls.find((call) => call.path === '/api/mcp')).to.include({
          status: 401,
        })
        expect(
          calls.find((call) => call.grantType === 'refresh_token')
        ).to.include({ status: 400 })
        expect(after.tokenGeneration).to.equal(before.tokenGeneration)
        expect(after.githubVisits).to.deep.equal(before.githubVisits)
        expect(after.connected).to.equal(false)
      })
    })
    return this
  },
  reconnect() {
    cy.task<{ authorizationUrl: string }>('assistantBegin').then(
      ({ authorizationUrl }) => {
        cy.visit(authorizationUrl)
      }
    )
    waitUntilAppIsNotBusy()
    return this
  },
  expectInformedConsent() {
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
    cy.findByRole('button', { name: 'Allow', exact: true }).should('be.visible')
    cy.findByRole('button', { name: 'Deny', exact: true }).should('be.visible')
    return this
  },
})
