/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import {
  Given,
  Then,
  When,
  type DataTable,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import notebookPage from '../start/pageObjects/notebookPage'
import { waitUntilAppIsNotBusy } from '../start/pageBase'

Given(
  'I have a notebook {string} with {int} numbered notes',
  (notebookName: string, count: number) => {
    const notes = Array.from({ length: count }, (_, index) => ({
      Title: `note-${String(index + 1).padStart(4, '0')}`,
      Content: 'seed',
    }))
    cy.get<string>('@currentLoginUser').then((username) =>
      start.testability().injectNotes(notes, username, notebookName)
    )
  }
)

Given('I choose to share my notebook {string}', (noteTopology: string) => {
  start.navigateToNotebookPage(noteTopology).shareNotebookToBazaar()
})

Then(
  'I should see readonly notebook {string} in my notes',
  (noteTopology: string) => {
    start
      .navigateToNotebooksPage()
      .subscribedNotebooks()
      .openNotebook(noteTopology)
    start
      .waitUntilAppIsNotBusy()
      .assumeNotePage()
      .addingChildNoteButton()
      .shouldNotExist()
  }
)

Then(
  'I should be able to edit the subscription to notebook {string}',
  (noteTopology: string) => {
    start
      .navigateToNotebooksPage()
      .notebookCard(noteTopology)
      .updateSubscription()
  }
)

When('I change notebook {string} to skip recall', (noteTopology: string) => {
  start.navigateToNotebookPage(noteTopology).skipMemoryTracking()
})

Then('I unsubscribe from notebook {string}', (noteTopology: string) => {
  start.navigateToNotebooksPage().notebookCard(noteTopology).unsubscribe()
})

When(
  'I add questions to the following notes in the notebook {string}',
  (_notebook: string, data: DataTable) => {
    data.rows().forEach((row) => {
      start.jumpToNotePage(row[0] as string).addQuestion({
        Stem: row[1] as string,
        'Choice 0': 'yes',
        'Choice 1': 'no',
        'Choice 2': 'maybe',
        'Correct Choice Index': '0',
      })
    })
  }
)

When(
  'I open the notebook {string} from my notebooks catalog',
  (notebookName: string) => {
    start.navigateToNotebookPage(notebookName)
  }
)

When('I export notebook {string} from the catalog', (notebookName: string) => {
  start.navigateToNotebooksPage().notebookCard(notebookName).exportNotebook()
})

Then(
  'a zip file for notebook {string} should be downloaded',
  (notebookName: string) => {
    const downloadsFolder = Cypress.config('downloadsFolder')
    const filePath = `${downloadsFolder}/${notebookName}.zip`
    cy.task('fileShouldExistSoon', filePath).should('equal', filePath)
    cy.readFile(filePath, 'binary').then((content: string) => {
      expect(content.startsWith('PK')).to.equal(true)
    })
  }
)

When(
  'I type notebook readme body {string} on the notebook page and save',
  (body: string) => {
    notebookPage().typeNotebookReadmeDraftAndSave(body)
  }
)

When('I reload the notebook page', () => {
  cy.reload()
  waitUntilAppIsNotBusy()
})

Then('the notebook readme body includes {string}', (fragment: string) => {
  notebookPage().expectNotebookReadmeBodyContains(fragment)
})

Then('I should see popup {string}', (message: string) => {
  // Wait for and verify alert message
  cy.on('window:alert', (text) => {
    expect(text).to.equal(message)
  })
})

Then('the notebook page summary shows name {string}', (name: string) => {
  cy.get('[data-testid="notebook-page-summary"]')
    .find('h1')
    .should('contain.text', name)
})

When('I open the notebook Health tab', () => {
  notebookPage().openHealthTab()
})

When('I run notebook health lint', () => {
  notebookPage().runLint()
})

When('I apply notebook health empty folder fix', () => {
  notebookPage().applyFix()
})

When('I check Remove empty folders on the notebook health panel', () => {
  notebookPage().checkRemoveEmptyFolders()
})

When('I save notebook health options as defaults', () => {
  notebookPage().saveAsDefaults()
})

Then('the notebook health idle prompt is visible', () => {
  notebookPage().expectHealthIdle()
})

Then('Remove empty folders on the notebook health panel is checked', () => {
  notebookPage().expectRemoveEmptyFoldersChecked()
})

Then(
  'the notebook health findings show expandable groups for empty folders, readme-only folders, and dead wiki links',
  () => {
    notebookPage().expectFindingGroupsExpandable()
  }
)

Then(
  'the notebook health empty folders finding includes {string}',
  (label: string) => {
    notebookPage().expectFindingGroupIncludes('empty_folders', label)
  }
)

Then(
  'the notebook health empty folders finding does not include {string}',
  (label: string) => {
    notebookPage().expectFindingGroupDoesNotInclude('empty_folders', label)
  }
)

Then(
  'the notebook health readme-only folders finding includes {string}',
  (label: string) => {
    notebookPage().expectFindingGroupIncludes('readme_only_folders', label)
  }
)

Then(
  'the notebook health dead wiki links finding includes note {string} and token {string}',
  (noteTitle: string, token: string) => {
    notebookPage().expectDeadWikiLinkFinding(noteTitle, token)
  }
)

When(
  'I rename the notebook from the notebook page summary to {string}',
  (newName: string) => {
    notebookPage().renameFromSummary(newName)
  }
)
