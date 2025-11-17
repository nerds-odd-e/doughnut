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
  cy.get('@savedInvitationCode')
    .invoke('toString')
    .then((url) => {
      cy.visit(url)
    })
})

When('I join the circle', () => {
  cy.get('input[value="Join"]').click()
})

When(
  'I should see the circle {string} and it has two members in it',
  (circleName: string) => {
    start.navigateToCircle(circleName).haveMembers(2)
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
  }
)

When(
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
      .navigateToChild(parentNoteTitle)
      .addingChildNote()
      .createNoteWithTitle(noteTopology)
  }
)

When(
  'I subscribe to notebook {string} in the circle {string}, with target of learning {int} notes per day',
  (notebookTitle: string, circleName: string, count: string) => {
    start.navigateToCircle(circleName).subscribe(notebookTitle, count)
  }
)

When('I am on {string} circle page', (circleName: string) => {
  start.navigateToCircle(circleName)
})

When(
  'There is a notebook {string} in circle {string} by {string}',
  (title: string, circleName: string, externalIdentifier: string) => {
    start
      .testability()
      .injectNotes([{ Title: title }], externalIdentifier, circleName)
  }
)

Then(
  'I move the notebook {string} from {string} to {string}',
  (notebook: string, fromCircle: string, toCircle: string) => {
    start.navigateToCircle(fromCircle).moveNotebook(notebook).toCircle(toCircle)
  }
)
