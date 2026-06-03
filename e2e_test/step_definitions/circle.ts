/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I create a new circle {string} and copy the invitation code',
  (circleName: string) => {
    start.navigateToMyCircles().createNewCircle(circleName).copyInvitationCode()
  }
)

When('I visit the invitation link', () => {
  cy.get<string>('@circleInvitationCode').then((code) => {
    cy.get('@savedInvitationCode')
      .invoke('toString')
      .then((url) => {
        cy.visit(url)
        start.pageIsNotLoading()
        cy.get('body').then(($body) => {
          if ($body.find('#username').length > 0) {
            cy.get<string>('@currentLoginUser').then((loginUser) => {
              if (!loginUser || loginUser === 'none') {
                return
              }
              cy.intercept('GET', '**/api/healthcheck').as('devLogin')
              cy.get('#username').clear().type(loginUser)
              cy.get('#password').clear().type('password')
              cy.get('#login-button').click()
              cy.wait('@devLogin').then(({ response }) => {
                expect(response?.statusCode, 'dev login on invite').to.equal(
                  200
                )
              })
              cy.url({ timeout: 15000 }).should('include', '/circles/join/')
              start.pageIsNotLoading()
              cy.get('#join-circle-invitationCode', {
                timeout: 15000,
              }).should(($input) => {
                expect($input.val()).to.equal(code)
              })
            })
            return
          }
          cy.get('#join-circle-invitationCode', { timeout: 15000 }).should(
            ($input) => {
              expect($input.val()).to.equal(code)
            }
          )
        })
      })
  })
})

When('I join the circle', () => {
  cy.intercept('POST', '**/api/circles/join').as('joinCircle')
  cy.get<string>('@circleInvitationCode').then((code) => {
    cy.get('@savedInvitationCode')
      .invoke('toString')
      .then((url) => {
        cy.visit(url)
        start.pageIsNotLoading()
        cy.get('#username, #join-circle-invitationCode', {
          timeout: 15000,
        }).should('exist')
        cy.get('body')
          .then(($body) => {
            if ($body.find('#username').length === 0) {
              return cy
                .get('#join-circle-invitationCode', { timeout: 15000 })
                .clear()
                .type(code)
            }
            return cy.get<string>('@currentLoginUser').then((loginUser) => {
              expect(loginUser, 'login before join')
                .to.be.a('string')
                .and.not.equal('none')
              cy.intercept('GET', '**/api/healthcheck').as('devLogin')
              cy.get('#username').clear().type(loginUser)
              cy.get('#password').clear().type('password')
              cy.get('#login-button').click()
              cy.wait('@devLogin').then(({ response }) => {
                expect(response?.statusCode, 'dev login before join').to.equal(
                  200
                )
              })
              cy.url({ timeout: 15000 }).should('include', '/circles/join/')
              start.pageIsNotLoading()
              return cy
                .get('#join-circle-invitationCode', { timeout: 15000 })
                .clear()
                .type(code)
            })
          })
          .then(() => {
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
    start
      .assumeCirclePage()
      .navigateToNotebook(parentNoteTitle)
      .addingNewNoteFromToolbar()
      .createNoteWithTitle(noteTopology)
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
