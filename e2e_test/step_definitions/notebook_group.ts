/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I create a notebook group named {string} by moving owned notebook {string} from the catalog',
  (groupName: string, notebookTitle: string) => {
    start
      .navigateToNotebooksPage()
      .creatingNotebookGroupFromCatalogMove(notebookTitle, groupName, false)
  }
)

When(
  'I create a notebook group named {string} by moving subscribed notebook {string} from the catalog',
  (groupName: string, notebookTitle: string) => {
    start
      .navigateToNotebooksPage()
      .creatingNotebookGroupFromCatalogMove(notebookTitle, groupName, true)
  }
)

When('I go to my notebooks page', () => {
  start.navigateToNotebooksPage()
})

When(
  'I open notebook group {string} from the catalog header',
  (groupName: string) => {
    start.navigateToNotebooksPage().openNotebookGroupFromHeader(groupName)
  }
)

Then(
  'I should see notebook group {string} with a hint including {string}',
  (groupName: string, hintSubstring: string) => {
    start
      .navigateToNotebooksPage()
      .expectNotebookGroupWithMemberHint(groupName, hintSubstring)
  }
)

Then(
  'I should be on the notebook group page for {string} with notebook {string} listed',
  (groupName: string, notebookTitle: string) => {
    cy.url().should('match', /\/d\/notebooks\/groups\/\d+/)
    cy.contains('h1', groupName).should('be.visible')
    cy.get('main').within(() => {
      cy.contains('h5', notebookTitle).should('be.visible')
    })
  }
)

When(
  'I set notebook {string} to ungrouped from the catalog',
  (notebookTitle: string) => {
    start.navigateToNotebooksPage().moveOwnedNotebookToUngrouped(notebookTitle)
  }
)

Then(
  'notebook {string} should appear at the top level of the notebook catalog',
  (notebookTitle: string) => {
    start
      .navigateToNotebooksPage()
      .expectNotebookAtTopLevelOfCatalog(notebookTitle)
  }
)
