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
import type NotePath from '../support/NotePath'
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
  'I should see readonly notebook {string} in the notebook catalog',
  (noteTopology: string) => {
    start
      .navigateToNotebooksPage()
      .subscribedNotebooks()
      .openNotebook(noteTopology)
    start.waitUntilAppIsNotBusy().assumeNotePage().expectCannotEditNotes()
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

When(
  'I change notebook {string} to skip memory tracking',
  (noteTopology: string) => {
    start.navigateToNotebookPage(noteTopology).skipMemoryTracking()
  }
)

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
  'I open the notebook {string} from the notebook catalog',
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

When('I save notebook readme {string}', (body: string) => {
  notebookPage().saveNotebookReadme(body)
})

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
  notebookPage().expectSummaryName(name)
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

When('I enable removing empty folders on the notebook health panel', () => {
  notebookPage().enableRemovingEmptyFolders()
})

When('I save notebook health options as defaults', () => {
  notebookPage().saveAsDefaults()
})

Then('the notebook health idle prompt is visible', () => {
  notebookPage().expectHealthIdle()
})

Then('removing empty folders on the notebook health panel is enabled', () => {
  notebookPage().expectRemovingEmptyFoldersEnabled()
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

When('I rename the notebook to {string}', (newName: string) => {
  notebookPage().rename(newName)
})

When(
  'I create a notebook with title {string} and description {string}',
  (notebookName: string, description: string) => {
    start.navigateToNotebooksPage().creatingNotebook(notebookName, description)
  }
)

When('I create a notebook with empty title', () => {
  start.navigateToNotebooksPage().creatingNotebook('')
})

Then('I should see that the notebook creation is not successful', () => {
  start.form.getField('Title').expectError('must not be blank')
})

When('I jump to the notebook {string}', (notebookName: string) => {
  start.jumpToNotebookPage(notebookName)
})

Then('I should see notebooks:', (data: DataTable) => {
  start.navigateToNotebooksPage().expectNotebookCards(data.hashes())
})

Then('I should be on a notebook folder page', () => {
  start.waitUntilAppIsNotBusy()
  cy.location('pathname').should('match', /^\/notebooks\/\d+\/folders\/\d+$/)
})

Then('I should be on the notebook root page', () => {
  start.waitUntilAppIsNotBusy()
  cy.location('pathname').should('match', /^\/notebooks\/\d+$/)
})

When('I navigate to {notepath} note', (notePath: NotePath) => {
  start.navigateToNoteFromPath(notePath)
})
