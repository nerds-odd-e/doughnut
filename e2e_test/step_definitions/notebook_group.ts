/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I create a notebook group named {string} from my notebooks page',
  (name: string) => {
    start.navigateToNotebooksPage().creatingNotebookGroup(name)
  }
)

When(
  'I assign notebook {string} to notebook group {string}',
  (notebookTitle: string, groupName: string) => {
    start.navigateToNotebookPage(notebookTitle).assignNotebookToGroup(groupName)
  }
)

When('I go to my notebooks page', () => {
  start.navigateToNotebooksPage()
})

Then(
  'I should see notebook group {string} with a hint including {string}',
  (groupName: string, hintSubstring: string) => {
    start
      .navigateToNotebooksPage()
      .expectNotebookGroupWithMemberHint(groupName, hintSubstring)
  }
)

When(
  'I set notebook {string} to ungrouped on the notebook settings page',
  (notebookTitle: string) => {
    start.navigateToNotebookPage(notebookTitle).setNotebookUngrouped()
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
