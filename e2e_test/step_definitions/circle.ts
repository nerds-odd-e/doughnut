/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { pageIsNotLoading } from '../start/pageBase'
import { circleNotebookIdAlias } from '../start/pageObjects/circlePage'
import notebookPage from '../start/pageObjects/notebookPage'

When(
  'I create a new circle {string} and copy the invitation code',
  (circleName: string) => {
    start.navigateToMyCircles().createNewCircle(circleName).copyInvitationCode()
  }
)

When('I visit the invitation link', () => {
  cy.get('@savedInvitationCode')
    .invoke('toString')
    .then((url) => {
      cy.visit(url)
      start.pageIsNotLoading()
      cy.get('body').then(($body) => {
        if ($body.find('#username').length > 0) {
          return
        }
        cy.get('#join-circle-invitationCode', { timeout: 15000 }).should(
          'exist'
        )
      })
    })
})

When('I join the circle', () => {
  cy.intercept('POST', '**/api/circles/join').as('joinCircle')
  cy.get<string>('@circleInvitationCode').then((code) => {
    expect(code, 'circle invitation code from inject').to.be.a('string').and.not
      .be.empty
    cy.get('#join-circle-invitationCode', { timeout: 15000 }).clear().type(code)
    cy.get('input[value="Join"]').click()
    cy.wait('@joinCircle').then(({ response }) => {
      expect(
        response?.statusCode,
        `join circle failed: ${JSON.stringify(response?.body)}`
      ).to.equal(200)
    })
    cy.url({ timeout: 15000 }).should('match', /\/circles\/\d+/)
    start.pageIsNotLoading()
  })
})

Then(
  'I should see the circle {string} and it has two members in it',
  (circleName: string) => {
    cy.findByText(`Circle: ${circleName}`)
    start.assumeCirclePage().haveMembers(2)
  }
)

Given(
  'circle {string} exists for {string} with invitation link saved',
  (circleName: string, memberExternalId: string) => {
    return start
      .testability()
      .injectCircle({ circleName, members: memberExternalId })
  }
)

Given(
  'There is a circle {string} with {string} members',
  (circleName: string, members: string) => {
    start
      .testability()
      .injectCircle({ circleName: circleName, members: members })
  }
)

When(
  'I create a notebook {string} in circle {string}',
  (noteTopology: string, circleName: string) => {
    start.navigateToCircle(circleName).creatingNotebook(noteTopology)
    cy.url().should('match', /\/notebooks\/\d+/)
    cy.url().then((url) => {
      const notebookId = url.match(/\/notebooks\/(\d+)/)?.[1]
      expect(
        notebookId,
        `expected notebook id in URL after creating "${noteTopology}"`
      ).to.exist
      cy.wrap(notebookId).as(circleNotebookIdAlias(noteTopology))
    })
    start.pageIsNotLoading().assumeNotePage().expectBreadcrumb(circleName)
  }
)

Then(
  'I should see the notebook {string} in circle {string}',
  (notebook: string, circleName: string) => {
    start.navigateToCircle(circleName).expectNotebooks(notebook)
  }
)

When(
  'I add a note {string} under {string}',
  (noteTopology: string, parentNoteTitle: string) => {
    const notebookAlias = circleNotebookIdAlias(parentNoteTitle)
    cy.wrap(null).then(() => {
      const aliases = Cypress.state('aliases') as
        | Record<string, unknown>
        | undefined
      if (aliases?.[notebookAlias]) {
        return cy
          .get(`@${notebookAlias}`, { log: false })
          .then((notebookId) => {
            cy.visit(`/notebooks/${notebookId}`)
            pageIsNotLoading()
            return notebookPage()
              .addingNewNoteFromToolbar()
              .createNoteWithTitle(noteTopology)
          })
      }
      return start
        .assumeCirclePage()
        .navigateToNotebook(parentNoteTitle)
        .addingNewNoteFromToolbar()
        .createNoteWithTitle(noteTopology)
    })
  }
)

When(
  'I subscribe to notebook {string} in the circle {string}, with target of learning {int} notes per day',
  (notebookName: string, circleName: string, count: string) => {
    start.navigateToCircle(circleName).subscribe(notebookName, count)
  }
)

When('I am on {string} circle page', (circleName: string) => {
  start.navigateToCircle(circleName)
})

Then('I should see circle notebook catalog layout controls', () => {
  start.assumeCirclePage().expectCatalogLayoutControls()
})

When(
  'I create a notebook group named {string} by moving notebook {string} from the circle catalog',
  (groupName: string, notebookName: string) => {
    start
      .assumeCirclePage()
      .creatingNotebookGroupFromCatalogMove(notebookName, groupName)
  }
)

Then(
  'I should see notebook group {string} with a hint including {string} on the circle page',
  (groupName: string, hintSubstring: string) => {
    cy.contains('[data-cy="notebook-group-card"]', groupName).should(
      'contain.text',
      hintSubstring
    )
  }
)

When(
  'There is a notebook {string} in circle {string} by {string}',
  (title: string, circleName: string, externalIdentifier: string) => {
    start
      .testability()
      .injectNotes([{ Title: title }], externalIdentifier, title, circleName)
  }
)

Given(
  'There is a circle {string} with {string} members and notebook {string} shared to the Bazaar by {string}',
  (
    circleName: string,
    members: string,
    notebookTitle: string,
    externalIdentifier: string
  ) => {
    start
      .testability()
      .injectCircle({ circleName, members })
      .then(() =>
        start
          .testability()
          .injectNotes(
            [{ Title: notebookTitle }],
            externalIdentifier,
            notebookTitle,
            circleName
          )
      )
      .then(() => start.testability().shareToBazaar(notebookTitle))
  }
)

Then(
  'I move the notebook {string} from {string} to {string}',
  (notebook: string, fromCircle: string, toCircle: string) => {
    start.navigateToCircle(fromCircle).moveNotebook(notebook).toCircle(toCircle)
  }
)
