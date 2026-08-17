/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { notebooksPage } from '../start/pageObjects/notebooksPage'
import notebookGroupPage from '../start/pageObjects/notebookGroupPage'

When(
  'I create a notebook group named {string} by moving owned notebook {string} from the catalog',
  (groupName: string, notebookName: string) => {
    start
      .navigateToNotebooksPage()
      .creatingNotebookGroupFromOwnedCatalogMove(notebookName, groupName)
  }
)

When(
  'I create a notebook group named {string} by moving subscribed notebook {string} from the catalog',
  (groupName: string, notebookName: string) => {
    start
      .navigateToNotebooksPage()
      .creatingNotebookGroupFromSubscribedCatalogMove(notebookName, groupName)
  }
)

When(
  'I open notebook group {string} from the catalog header',
  (groupName: string) => {
    notebooksPage().openNotebookGroupFromHeader(groupName)
  }
)

When(
  'I add a notebook {string} to notebook group {string} from the catalog',
  (notebookName: string, groupName: string) => {
    start
      .navigateToNotebooksPage()
      .addingNotebookToGroupFromCatalog(groupName, notebookName)
  }
)

Then(
  'I should see notebook group {string} with a hint including {string}',
  (groupName: string, hintSubstring: string) => {
    notebooksPage().expectNotebookGroupWithMemberHint(groupName, hintSubstring)
  }
)

Then(
  'I should be on the notebook group page for {string} with notebook {string} listed',
  (groupName: string, notebookName: string) => {
    notebookGroupPage().expectGroupWithNotebookListed(groupName, notebookName)
  }
)

When(
  'I set notebook {string} to ungrouped from the catalog',
  (notebookName: string) => {
    start.navigateToNotebooksPage().moveOwnedNotebookToUngrouped(notebookName)
  }
)

Then(
  'notebook {string} should appear at the top level of the notebook catalog',
  (notebookName: string) => {
    notebooksPage().expectNotebookAtTopLevelOfCatalog(notebookName)
  }
)
