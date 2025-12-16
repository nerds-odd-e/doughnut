/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type NotePath from '../support/NotePath'
import start from '../start'
import { commonSenseSplit } from '../support/string_util'

When('I start searching from all my notebooks page', () => {
  start.routerToNotebooksPage()
  start.assumeNotePage().toolbarButton('search note').click()
})

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
  'I add relationship from top level note {string} as {string} to note {string}',
  (fromNoteTopic: string, relationType: string, toNoteTopic: string) => {
    start
      .jumpToNotePage(fromNoteTopic)
      .startSearchingAndAddRelationship()
      .findTarget(toNoteTopic)
      .createRelationshipToTopLevelNoteAs(toNoteTopic, relationType)
  }
)

When(
  'I move note {string} to be under note {string}',
  (fromNoteTopic: string, toNoteTopic: string) => {
    start
      .jumpToNotePage(fromNoteTopic)
      .startSearchingAndAddRelationship()
      .findTarget(toNoteTopic)
      .moveUnder()
  }
)

When(
  'there is {string} relationship between note {string} and {string}',
  (relationType: string, fromNoteTopic: string, toNoteTopic: string) => {
    start
      .testability()
      .injectRelationship(relationType, fromNoteTopic, toNoteTopic)
  }
)

When(
  'I should see {string} as the possible duplicate',
  (noteTopicsAsString: string) => {
    cy.tick(1000)
    start
      .assumeNoteTargetSearchDialog()
      .expectExactDropdownTargets(
        commonSenseSplit(noteTopicsAsString, ',').map((i: string) => i.trim())
      )
  }
)

When(
  'I should see {string} as targets only when searching {string}',
  (noteTopicsAsString: string, searchKey: string) => {
    start
      .assumeNoteTargetSearchDialog()
      .findTargetWithinNotebook(searchKey)
      .expectExactRelationshipTargets(
        commonSenseSplit(noteTopicsAsString, ',').map((i: string) => i.trim())
      )
  }
)

When(
  'I should see {string} as targets only when searching in all my notebooks {string}',
  (noteTopicsAsString: string, searchKey: string) => {
    start
      .assumeNoteTargetSearchDialog()
      .findTarget(searchKey)
      .expectExactRelationshipTargets(
        commonSenseSplit(noteTopicsAsString, ',').map((i: string) => i.trim())
      )
  }
)

When(
  'I should see note cannot be found when searching in all my notebooks {string}',
  (searchKey: string) => {
    start.assumeNoteTargetSearchDialog().findTarget(searchKey)
    cy.findByText('No matching notes found.').should('be.visible')
  }
)

When('I search for {string} in all my notebooks', (searchKey: string) => {
  start.assumeNoteTargetSearchDialog().findTarget(searchKey)
})

Then(
  'I should see {string} in the recently updated notes section',
  (noteTitle: string) => {
    start
      .assumeNoteTargetSearchDialog()
      .expectNoteInRecentlyUpdatedSection(noteTitle)
  }
)

Then('I should not see the recently updated notes section', () => {
  start.assumeNoteTargetSearchDialog().expectRecentlyUpdatedSectionNotVisible()
})

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
      .routerToNotebooksPage()
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
  'I change the relationship from {string} to {string} to {string}',
  (noteTopology: string, targetTitle: string, relationType: string) => {
    start
      .jumpToNotePage(noteTopology)
      .navigateToRelationshipChild(targetTitle)
      .changeRelationType(relationType, targetTitle)
  }
)

Then(
  'I change the reference from {string} to {string} to {string}',
  (noteTopology: string, referenceTitle: string, relationType: string) => {
    start
      .jumpToNotePage(noteTopology)
      .navigateToReference(referenceTitle)
      .changeRelationType(relationType, noteTopology)
  }
)

Then('I should be able to delete the relationship', () => {
  cy.findByRole('button', { name: 'Delete' }).click()
})

Then(
  'I delete the relationship from {string} to {string}',
  (noteTopology: string, targetTitle: string) => {
    start
      .jumpToNotePage(noteTopology)
      .navigateToRelationshipChild(targetTitle)
      .deleteNote()
    start.assumeNotePage(noteTopology) // remain on the same note page
  }
)
