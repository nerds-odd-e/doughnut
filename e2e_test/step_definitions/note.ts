/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Given,
  Then,
  When,
  defineParameterType,
} from '@badeball/cypress-cucumber-preprocessor'
import NotePath from '../support/NotePath'
import '../support/string_util'
import start from '../start'
import mock_services from '../start/mock_services'

defineParameterType({
  name: 'notepath',
  regexp: /.*/,
  transformer(s: string) {
    return new NotePath(s)
  },
})

function omitBlankOptionalInjectionFields(rows: Record<string, string>[]) {
  return rows.map((row) => {
    const next = { ...row }
    for (const [key, value] of Object.entries(next)) {
      if (key === 'Title') {
        continue
      }
      if (typeof value === 'string' && value.trim() === '') {
        delete next[key]
      }
    }
    return next
  })
}

Given(
  'I have a notebook {string} with notes:',
  (notebookName: string, data: DataTable) => {
    const notes = omitBlankOptionalInjectionFields(data.hashes())
    cy.get<string>('@currentLoginUser').then((username) =>
      start.testability().injectNotes(notes, username, notebookName)
    )
  }
)

Given('I have a notebook {string}', (notebookName: string) => {
  cy.get<string>('@currentLoginUser').then((username) =>
    start.testability().injectNotes([], username, notebookName)
  )
})

Given(
  'I have a notebook {string} with a note {string}',
  (notebookName: string, noteTitle: string) => {
    cy.get<string>('@currentLoginUser').then((username) =>
      start
        .testability()
        .injectNotes([{ Title: noteTitle }], username, notebookName)
    )
  }
)

Given(
  'I have a notebook {string} with a note {string} which skips memory tracking',
  (notebookName: string, noteTitle: string) => {
    cy.get<string>('@currentLoginUser').then((username) =>
      start.testability().injectNotes(
        [
          {
            Title: noteTitle,
            'Skip Memory Tracking': true,
          },
        ],
        username,
        notebookName
      )
    )
  }
)

Given(
  'I have a notebook {string} with a note {string} and details {string}',
  (notebookName: string, noteTitle: string, details: string) => {
    cy.get<string>('@currentLoginUser').then((username) =>
      start.testability().injectNotes(
        [
          {
            Title: noteTitle,
            Details: details,
          },
        ],
        username,
        notebookName
      )
    )
  }
)

Given(
  'there are some notes for existing user {string} in notebook {string}',
  (externalIdentifier: string, notebookName: string, data: DataTable) => {
    const hashes = data
      .hashes()
      .map((row) =>
        Object.fromEntries(
          Object.entries(row).map(([key, value]) => [key.trim(), value])
        )
      )
    start.testability().injectNotes(hashes, externalIdentifier, notebookName)
  }
)

Given(
  'there is a notebook {string} with a note {string} from user {string} shared to the Bazaar',
  (notebookName: string, noteTitle: string, externalIdentifier: string) => {
    start
      .testability()
      .injectNotes([{ Title: noteTitle }], externalIdentifier, notebookName)
      .then(() => {
        return start.testability().shareToBazaar(notebookName)
      })
  }
)

Given(
  'there are notes from Note {int} to Note {int}',
  (from: number, to: number) => {
    const notes = Array(to - from + 1)
      .fill(0)
      .map((_, i) => {
        return { Title: `Note ${i + from}` }
      })
    cy.get<string>('@currentLoginUser').then((username) =>
      start.testability().injectNotes(notes, username, `Note ${from}`)
    )
  }
)

Given(
  'there are questions in the notebook {string} for the note:',
  (notebook: string, data: DataTable) => {
    start.testability().injectPredefinedQuestionsToNotebook({
      notebookName: notebook,
      predefinedQuestionTestData: data.hashes(),
    })
  }
)

Given(
  'I add the following question for the note {string}:',
  (noteTopology: string, data: DataTable) => {
    expect(data.hashes().length, 'please add one question at a time.').to.equal(
      1
    )
    start.jumpToNotePage(noteTopology).addQuestion(data.hashes()[0]!)
  }
)

Given(
  'I refine the following question for the note {string}:',
  (noteTopology: string, data: DataTable) => {
    expect(data.hashes().length, 'please add one question at a time.').to.equal(
      1
    )
    start.jumpToNotePage(noteTopology).refineQuestion(data.hashes()[0]!)
  }
)

When(
  'I create a notebook with title {string} and description {string}',
  (notebookName: string, description: string) => {
    start.navigateToNotebooksPage().creatingNotebook(notebookName, description)
  }
)

When('I create a notebook with empty title', () => {
  start.navigateToNotebooksPage().creatingNotebook('')
})

When(
  'I update note {string} to become:',
  (noteTopology: string, data: DataTable) => {
    start.jumpToNotePage(noteTopology).editTextContent(data.hashes()[0]!)
  }
)

When(
  'I update note accessories of {string} to become:',
  (noteTopology: string, data: DataTable) => {
    start
      .jumpToNotePage(noteTopology)
      .updateNoteImage(data.hashes()[0]!)
      .updateNoteUrl(data.hashes()[0]!)
  }
)

When(
  'I should see note {string} has a image and a url {string}',
  (noteTopology: string, expectedUrl: string) => {
    start.jumpToNotePage(noteTopology)
    cy.get('#note-image').should('exist')
    cy.findByLabelText('Url:').should('have.attr', 'href', expectedUrl)
  }
)

When(
  'I can change the title {string} to {string}',
  (noteTopology: string, newNoteTitle: string) => {
    start.assumeNotePage(noteTopology).editTextContent({ title: newNoteTitle })
    start.assumeNotePage(newNoteTitle)
  }
)

Given(
  'I update note title {string} to become {string}',
  (noteTopology: string, newNoteTitle: string) => {
    start.jumpToNotePage(noteTopology).editTextContent({ title: newNoteTitle })
  }
)

Given(
  'I update note {string} details from {string} to become {string}',
  (noteTopology: string, noteDetails: string, newNoteDetails: string) => {
    cy.findByText(noteDetails).click({ force: true })
    start
      .assumeNotePage(noteTopology)
      .editTextContent({ Details: newNoteDetails })
  }
)

When(
  'I update note {string} with details {string}',
  (noteTopology: string, newDetails: string) => {
    start.jumpToNotePage(noteTopology).editTextContent({ Details: newDetails })
    start.assumeNotePage().findNoteDetails(newDetails)
  }
)

When(
  'I create a note belonging to {string} with title {string}',
  (noteTopology: string, title: string) => {
    start.jumpToNotePage(noteTopology)
    start.noteSidebar().activateFolderByLabel(noteTopology)
    start.noteSidebar().addingNewNoteFromToolbar().createNoteWithTitle(title)
    start.assumeNotePage(title)
    start.testability().rememberUiCreatedNote(title)
  }
)

When(
  'I create a note belonging to {string} with title {string} and wikidata id {string}',
  (noteTopology: string, title: string, wikidataId: string) => {
    mock_services.wikidata().stubWikidataSearchResult(title, wikidataId)
    start.jumpToNotePage(noteTopology)
    start.noteSidebar().activateFolderByLabel(noteTopology)
    start
      .noteSidebar()
      .addingNewNoteFromToolbar()
      .createNoteWithTitleAndWikidataId(title, wikidataId)
    // Wikidata creation enriches siblings (authors, country); backend work can exceed default 6s.
    start.assumeNotePage(title, { timeout: 30000 })
    start.testability().rememberUiCreatedNote(title)
  }
)

When(
  'I attempt to create a note belonging to {string} with title {string} and wikidata id {string}',
  (noteTopology: string, title: string, wikidataId: string) => {
    mock_services.wikidata().stubWikidataSearchResult(title, wikidataId)
    start.jumpToNotePage(noteTopology)
    start.noteSidebar().activateFolderByLabel(noteTopology)
    start
      .noteSidebar()
      .addingNewNoteFromToolbar()
      .createNoteWithTitleAndWikidataId(title, wikidataId)
  }
)

When('I am creating a note under {notepath}', (notePath: NotePath) => {
  start
    .navigateToNotebooksPage()
    .navigateToPath(notePath)
    .addingNewNoteFromToolbar()
})

Then('I should see {string} in breadcrumb', (noteTitles: string) => {
  start.pageIsNotLoading().assumeNotePage().expectBreadcrumb(noteTitles)
})

Then('the note title should be {string}', (title: string) => {
  start.assumeNotePage().expectNoteTitleDisplayed(title)
})

Then('the note details should include {string}', (fragment: string) => {
  start.assumeNotePage().expectDetailsContaining(fragment)
})

When('I visit all my notebooks', () => {
  start.navigateToNotebooksPage()
})

Then('I should see my notebooks:', (data: DataTable) => {
  start.navigateToNotebooksPage().expectNotebookCards(data.hashes())
})

Then(
  'I should see folder {notepath} containing these notes:',
  (notePath: NotePath, data: DataTable) => {
    start
      .pageIsNotLoading()
      .navigateToNotebooksPage()
      .openFolder(notePath)
      .expectChildrenUnderSidebarFolder(data.hashes())
  }
)

When(
  'I create a folder named {string} while viewing note {string}',
  (folderName: string, noteTitle: string) => {
    start.jumpToNotePage(noteTitle)
    start
      .noteSidebar()
      .addingNewFolderFromToolbar()
      .createFolderWithName(folderName)
  }
)

Then('I should see sidebar folder {string}', (folderLabel: string) => {
  start.noteSidebar().expectSidebarFolderVisible(folderLabel)
})

Then(
  'I should see sidebar folder {string} under folder {string}',
  (childFolderLabel: string, parentFolderLabel: string) => {
    start
      .noteSidebar()
      .expectSidebarFolderUnderParent(parentFolderLabel, childFolderLabel)
  }
)

Then(
  'I should not see sidebar folder {string} under folder {string}',
  (childFolderLabel: string, parentFolderLabel: string) => {
    start
      .noteSidebar()
      .expectSidebarFolderNotUnderParent(parentFolderLabel, childFolderLabel)
  }
)

Then(
  'I should see note {string} under folder {string}',
  (noteTitle: string, folderLabel: string) => {
    start.noteSidebar().expectSidebarNoteUnderFolder(folderLabel, noteTitle)
  }
)

When(
  'I delete note {string} at {int}:00',
  (noteTopology: string, hour: number) => {
    start.testability().backendTimeTravelTo(0, hour)
    start.jumpToNotePage(noteTopology).deleteNote()
  }
)

When('I delete note {string}', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).deleteNote()
})

When('I should see that the note creation is not successful', () => {
  start.form.getField('Title').expectError('must not be blank')
  cy.get('.Vue-Toastification__close-button').click()
})

Then(
  'I should see the note {string} is marked as deleted',
  (noteTopology: string) => {
    start.jumpToNotePage(noteTopology)
    cy.findByText('This note has been deleted')
  }
)

When('I navigate to {notepath} note', (notePath: NotePath) => {
  start.navigateToNoteFromPath(notePath)
})

// This step definition is for demo purpose
Then(
  '*for demo* I should see there are {int} descendants',
  (numberOfDescendants: number) => {
    cy.findByText(`${numberOfDescendants}`, {
      selector: '.descendant-counter',
    })
  }
)

Then(
  'I should see {string} is {string} than {string}',
  (left: string, aging: string, right: string) => {
    let leftColor = ''
    start.pageIsNotLoading().jumpToNotePage(left)
    cy.get('.note-recent-update-indicator')
      .invoke('css', 'color')
      .then((val) => {
        leftColor = val as unknown as string
      })
    start.jumpToNotePage(right)
    cy.get('.note-recent-update-indicator')
      .invoke('css', 'color')
      .then((val) => {
        const leftColorIndex = parseInt(leftColor.match(/\d+/)![0])
        const rightColorIndex = parseInt(
          JSON.stringify(val).match(/\d+/)?.[0] ?? ''
        )
        if (aging === 'newer') {
          expect(leftColorIndex).to.greaterThan(rightColorIndex)
        } else {
          expect(leftColorIndex).to.equal(rightColorIndex)
        }
      })
  }
)

When('I undo {string}', (undoType: string) => {
  start.assumeNotePage().undo(undoType)
  start.pageIsNotLoading()
})

When('I undo {string} again', (undoType: string) => {
  start.assumeNotePage().undo(undoType)
})

When('I undo delete note to recover note {string}', (noteTitle: string) => {
  start.assumeNotePage().undo('delete note')
  start.assumeNotePage(noteTitle)
})

When(
  'I undo delete note to recover note {string} again',
  (noteTitle: string) => {
    start.assumeNotePage().undo('delete note')
    start.assumeNotePage(noteTitle)
  }
)

Then('there should be no more undo to do', () => {
  cy.get('.btn[title="undo"]').should('not.exist')
})

Then('I type {string} in the title', (content: string) => {
  cy.focused().clear().type(content)
})

Then(
  'the note details on the current page should be {string}',
  (detailsText: string) => {
    start.assumeNotePage().findNoteDetails(detailsText)
  }
)

Then(
  'the note details on the current page should be {string} within {int} seconds',
  (detailsText: string, timeout: number) => {
    start.assumeNotePage().findNoteDetails(detailsText, timeout * 1000)
  }
)

When('I generate an image for {string}', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).aiGenerateImage()
})

Then('I should find an art created by the ai', () => {
  cy.get('img.ai-art').should('be.visible')
})

Given(
  'I request to complete the details for the note {string}',
  (noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .startAConversationAboutNote()
      .replyToConversationAndInviteAiToReply(
        'Please complete the note details.'
      )
    start.pageIsNotLoading()
  }
)

Then('I should see a notification of a bad request', () => {
  start.assumeConversationAboutNotePage().expectErrorMessage('Bad Request')
})

When('I start to chat about the note {string}', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).startAConversationAboutNote()
})

When('I expand the children of note {string}', (noteTopology: string) => {
  start.assumeNotePage(noteTopology).expandChildren()
})

When(
  'I expand the children of note {string} in the sidebar',
  (noteTopology: string) => {
    start.noteSidebar().expand(noteTopology)
  }
)

When('I route to the note {string}', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology)
})

When(
  'I should see the questions in the question list of the note {string}:',
  (noteTopology: string, data: DataTable) => {
    start.jumpToNotePage(noteTopology).expectQuestionsInList(data.hashes())
  }
)

When('I generate question by AI for note {string}', (noteName: string) => {
  start
    .jumpToNotePage(noteName)
    .openQuestionList()
    .addQuestionPage()
    .generateQuestionByAI()
})

Then('the question in the form becomes:', (data: DataTable) => {
  const expectedQuestions = data.hashes()[0]!
  ;['Stem', 'Choice 0', 'Choice 1', 'Choice 2', 'Correct Choice Index'].forEach(
    (key) => {
      cy.findByLabelText(key).should('have.value', expectedQuestions[key]!)
    }
  )
})

Given('I open the note {string} for editing', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology)
})

When(
  'I add a rich note property with key {string} and value {string}',
  (key: string, value: string) => {
    start.assumeNotePage().addRichNoteProperty(key, value)
  }
)

Then(
  'I should see rich note property {string} with value {string}',
  (key: string, value: string) => {
    start.assumeNotePage().expectRichNotePropertyDisplayed(key, value)
  }
)

Then('I should not see rich note property {string}', (key: string) => {
  start.assumeNotePage().expectRichNotePropertyAbsent(key)
})

When(
  'I edit the rich note property with key {string} to key {string} and value {string}',
  (oldKey: string, newKey: string, newValue: string) => {
    start.assumeNotePage().editRichNoteProperty(oldKey, newKey, newValue)
  }
)

When(
  'I update note {string} details using markdown to become:',
  (noteTopology: string, newDetails: string) => {
    start.jumpToNotePage(noteTopology).updateDetailsAsMarkdown(newDetails)
  }
)

When(
  'I update the current note details using markdown to become:',
  (newDetails: string) => {
    start.assumeNotePage().updateDetailsAsMarkdown(newDetails)
  }
)

When('I flush pending note details save', () => {
  start.assumeNotePage().flushPendingDetailsSave()
})

When('I reload the current page for note {string}', (noteTopology: string) => {
  cy.reload()
  start.pageIsNotLoading()
  start.assumeNotePage(noteTopology)
})

When('I open the note details markdown editor', () => {
  start.assumeNotePage().openMarkdownDetailsEditor()
})

Then(
  'the note details markdown source should contain {string}',
  (fragment: string) => {
    start.assumeNotePage().expectMarkdownDetailsSourceContains(fragment)
  }
)

Then(
  'the note details markdown source should not contain {string}',
  (fragment: string) => {
    start.assumeNotePage().expectMarkdownDetailsSourceDoesNotContain(fragment)
  }
)

When('I view the note details as rich content', () => {
  start.assumeNotePage().switchToRichDetails()
})

When('I view the note details as markdown', () => {
  start.assumeNotePage().toolbarButton('Edit as markdown').click()
})

Then(
  'I should see the rich content elements in the note details:',
  (data: DataTable) => {
    start.assumeNotePage().expectRichDetails(data.hashes())
  }
)

Then(
  'I should see the rich content of the note with details:',
  (data: DataTable) => {
    start
      .assumeNotePage()
      .switchToRichContent()
      .expectRichDetails(data.hashes())
  }
)

When(
  'I activate folder {string} in the sidebar and create a new note with title {string}',
  (folderLabel: string, title: string) => {
    start.jumpToNotePage('team')
    start.noteSidebar().activateFolderByLabel(folderLabel)
    start.noteSidebar().addingNewNoteFromToolbar().createNoteWithTitle(title)
    start.assumeNotePage(title)
    start.testability().rememberUiCreatedNote(title)
  }
)
Then(
  'I should see note {notepath} has details {string}',
  (notePath: NotePath, expectedDetails: string) => {
    start
      .navigateToNotebooksPage()
      .navigateToPath(notePath)
      .findNoteDetails(expectedDetails)
  }
)

Then('the note details should contain a line break', () => {
  start.assumeNotePage().expectNoteDetailsContainLineBreak()
})

When('I promote the point {string} to a sibling note', (pointText: string) => {
  start.assumeAssimilationPage().promotePointToSiblingNote(pointText)
})

Then(
  'the link {string} should link to the note with the same title',
  (linkText: string) => {
    start
      .assumeNotePage()
      .wikiLinkInDetails(linkText)
      .expectNoteShowHref()
      .followAndAssumeNote(linkText)
  }
)

Then(
  'the link {string} should open the note titled {string}',
  (linkText: string, noteTitle: string) => {
    start
      .assumeNotePage()
      .wikiLinkInDetails(linkText)
      .expectNoteShowHref()
      .followAndAssumeNote(noteTitle)
  }
)

Then(
  'I should be able to create a new note by following the dead link {string}',
  (linkTitle: string) => {
    start.assumeNotePage().followDeadLink(linkTitle).createNote()
    start.assumeNotePage(linkTitle)
  }
)
