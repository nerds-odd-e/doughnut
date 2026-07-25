/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { commonSenseSplit } from '../support/string_util'

Given('I reindex the notebook {string}', (notebookName: string) => {
  start.testability().updateNotebookIndex(notebookName)
})

When('I start searching from all my notebooks page', () => {
  start.navigateToNotebooksPage()
  start.assumeNotePage().toolbarButton('search note').click()
})

When('I enable semantic search in the note search dialog', () => {
  start.assumeNoteTargetSearchDialog().enableSemanticSearch()
})

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

Then(
  'I should see notebook {string} in search results',
  (notebookName: string) => {
    start
      .assumeNoteTargetSearchDialog()
      .expectNotebookNameInSearchResults(notebookName)
  }
)

When(
  'I should see note cannot be found when searching in all my notebooks {string}',
  (searchKey: string) => {
    start
      .assumeNoteTargetSearchDialog()
      .findTarget(searchKey)
      .expectNoRelationshipTargetNotes()
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
  'I should see relationship targets {string}',
  (noteTopicsAsString: string) => {
    start
      .assumeNoteTargetSearchDialog()
      .expectExactRelationshipTargets(
        commonSenseSplit(noteTopicsAsString, ',').map((i: string) => i.trim())
      )
  }
)

When('I select Recent in the search list mode', () => {
  start.assumeNoteTargetSearchDialog().selectSearchListModeRecent()
})

When('I select Matches in the search list mode', () => {
  start.assumeNoteTargetSearchDialog().selectSearchListModeMatches()
})

Then('the note search field should contain {string}', (value: string) => {
  start.assumeNoteTargetSearchDialog().expectSearchFieldContains(value)
})
