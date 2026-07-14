/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type NotePath from '../support/NotePath'
import start from '../start'

function onRelationshipNote(noteTopology: string, targetTitle: string) {
  return start.jumpToNotePage(noteTopology).navigateToReference(targetTitle)
}

function givenInjectedRelationshipNote(
  relationType: string,
  fromNoteTopic: string,
  toNoteTopic: string,
  notebookName: string,
  bodySuffix?: string
) {
  start
    .testability()
    .injectRelationshipNote(
      notebookName,
      relationType,
      fromNoteTopic,
      toNoteTopic,
      bodySuffix
    )
}

When(
  'I am creating a relationship under note {string}',
  (noteTopology: string) => {
    start.jumpToNotePage(noteTopology).startSearchingAndAddRelationship()
  }
)

When(
  'I add relationship from note {string} as {string} to note {string}',
  (fromNoteTopic: string, relationType: string, toNoteTopic: string) => {
    start
      .jumpToNotePage(fromNoteTopic)
      .startSearchingAndAddRelationship()
      .findTarget(toNoteTopic)
      .createRelationshipToTargetAs(toNoteTopic, relationType)
  }
)

When(
  'I open the relationship from {string} to {string}',
  (sourceNoteTopology: string, targetTitle: string) => {
    onRelationshipNote(sourceNoteTopology, targetTitle)
  }
)

When(
  'I move note {string} to be under note {string}',
  (fromNoteTopic: string, toNoteTopic: string) => {
    start
      .jumpToNotePage(fromNoteTopic)
      .startSearchingAndAddRelationship()
      .findTarget(toNoteTopic)
      .moveUnder(toNoteTopic)
  }
)

Given(
  'there is {string} relationship between note {string} and {string} in notebook {string}',
  (
    relationType: string,
    fromNoteTopic: string,
    toNoteTopic: string,
    notebookName: string
  ) => {
    givenInjectedRelationshipNote(
      relationType,
      fromNoteTopic,
      toNoteTopic,
      notebookName
    )
  }
)

Given(
  'there is {string} relationship between note {string} and {string} in notebook {string} with body suffix:',
  (
    relationType: string,
    fromNoteTopic: string,
    toNoteTopic: string,
    notebookName: string,
    bodySuffix: string
  ) => {
    givenInjectedRelationshipNote(
      relationType,
      fromNoteTopic,
      toNoteTopic,
      notebookName,
      bodySuffix
    )
  }
)

Then(
  'I should see {string} has relationship {string} {string}',
  (noteTopology: string, relationType: string, targetNoteTopics: string) => {
    start
      .jumpToNotePage(noteTopology)
      .expectRelationshipChildren(relationType, targetNoteTopics)
  }
)

Then(
  'I should see note {notepath} has relationship {string} {string}',
  (notePath: NotePath, relationType: string, targetNoteTopics: string) => {
    start
      .navigateToNotebooksPage()
      .navigateToPath(notePath)
      .expectRelationshipChildren(relationType, targetNoteTopics)
  }
)

Then(
  'I should see {string} has no relationship to {string}',
  (noteTopology: string, targetTitle: string) => {
    start.jumpToNotePage(noteTopology)
    cy.findByText(targetTitle, { selector: 'main *' }).should('not.exist')
  }
)

Then(
  'I should be on the relationship note page from {string} with relation {string} to {string}',
  (sourceTitle: string, relationType: string, targetTitle: string) => {
    cy.findByRole('region', { name: 'Note content' }).should('be.visible')
    cy.findByRole('title')
      .should('contain', sourceTitle)
      .and('contain', relationType)
      .and('contain', targetTitle)
  }
)

When(
  'I change the relationship from {string} to {string} to {string}',
  (noteTopology: string, targetTitle: string, relationType: string) => {
    onRelationshipNote(noteTopology, targetTitle).changeRelationType(
      relationType
    )
  }
)

Then('I should be able to delete the relationship', () => {
  cy.findByRole('button', { name: 'Delete' }).click()
})

When(
  'I delete the relationship from {string} to {string}',
  (noteTopology: string, targetTitle: string) => {
    onRelationshipNote(noteTopology, targetTitle).deleteRelationshipNote()
  }
)

When(
  'I delete the relationship from {string} to {string} and reduce it to a property of the source',
  (noteTopology: string, targetTitle: string) => {
    onRelationshipNote(
      noteTopology,
      targetTitle
    ).deleteNoteAndReduceToSourceProperty()
  }
)
