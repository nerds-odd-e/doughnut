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
  'I have a notebook {string} with a note {string} and content {string}',
  (notebookName: string, noteTitle: string, content: string) => {
    cy.get<string>('@currentLoginUser').then((username) =>
      start.testability().injectNotes(
        [
          {
            Title: noteTitle,
            Content: content,
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
    return start
      .testability()
      .injectNotes(hashes, externalIdentifier, notebookName)
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

When(
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

When('I should see note {string} has an image', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology)
  cy.get('#note-image').should('exist')
})

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

When(
  'I set the note title to {string} keeping visible reference text',
  (newTitle: string) => {
    start
      .assumeNotePage()
      .saveReferencedNoteTitle(newTitle, 'KEEP_VISIBLE_TEXT')
  }
)

When(
  'I set the note title to {string} updating visible reference text',
  (newTitle: string) => {
    start
      .assumeNotePage()
      .saveReferencedNoteTitle(newTitle, 'UPDATE_VISIBLE_TEXT')
  }
)

Given(
  'I update note {string} content from {string} to become {string}',
  (noteTopology: string, previousContent: string, newContent: string) => {
    cy.findByText(previousContent).click({ force: true })
    start.assumeNotePage(noteTopology).editTextContent({ Content: newContent })
  }
)

When(
  'I update note {string} with content {string}',
  (noteTopology: string, newContent: string) => {
    start.jumpToNotePage(noteTopology).editTextContent({ Content: newContent })
    start.assumeNotePage().findNoteContent(newContent)
  }
)

When(
  'I create a note with title {string} under the folder {string} in the notebook {string}',
  (title: string, folder: string, notebook: string) => {
    start.navigateToNotebooksPage().navigateToNotebook(notebook)
    start
      .noteSidebar()
      .activateFolderByLabel(folder)
      .addingNewNoteFromToolbar()
      .createNoteWithTitle(title)
    start.assumeNotePage(title)
    if (title !== '') {
      start.testability().rememberUiCreatedNote(title)
    }
  }
)

When(
  'I create a note with title {string} and wikidata id {string} in the notebook {string}',
  (title: string, wikidataId: string, notebook: string) => {
    mock_services.wikidata().stubWikidataSearchResult(title, wikidataId)
    start
      .navigateToNotebooksPage()
      .navigateToNotebook(notebook)
      .addingNewNoteFromToolbar()
      .createNoteWithTitleAndWikidataId(title, wikidataId)
    start.assumeNotePage(title, { timeout: 30000 })
    start.testability().rememberUiCreatedNote(title)
  }
)

When(
  'I attempt to create a note with title {string} and wikidata id {string} in the notebook {string}',
  (title: string, wikidataId: string, notebook: string) => {
    mock_services.wikidata().stubWikidataSearchResult(title, wikidataId)
    start
      .navigateToNotebooksPage()
      .navigateToNotebook(notebook)
      .addingNewNoteFromToolbar()
      .createNoteWithTitleAndWikidataId(title, wikidataId)
  }
)

When('I am creating a note in the notebook {string}', (notebook: string) => {
  start
    .navigateToNotebooksPage()
    .navigateToNotebook(notebook)
    .addingNewNoteFromToolbar()
})

Then('I should see {string} in breadcrumb', (noteTitles: string) => {
  start.pageIsNotLoading().assumeNotePage().expectBreadcrumb(noteTitles)
})

Then('the note title should be {string}', (title: string) => {
  start.assumeNotePage().expectNoteTitleDisplayed(title)
})

Then('the note content should include {string}', (fragment: string) => {
  start.assumeNotePage().expectContentContaining(fragment)
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
      .expandFolderInSidebar(notePath)
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
  'I should see sidebar folder {string} under open folder {string}',
  (childFolderLabel: string, parentFolderLabel: string) => {
    start
      .noteSidebar()
      .expectSidebarFolderUnderOpenParent(parentFolderLabel, childFolderLabel)
  }
)

Then(
  'I should see sidebar folder {string} under collapsed folder {string}',
  (childFolderLabel: string, parentFolderLabel: string) => {
    start
      .noteSidebar()
      .expand(parentFolderLabel)
      .expectSidebarFolderUnderOpenParent(parentFolderLabel, childFolderLabel)
  }
)

Then(
  'I should see note {string} under open folder {string}',
  (noteTitle: string, folderLabel: string) => {
    start.noteSidebar().expectSidebarNoteUnderOpenFolder(folderLabel, noteTitle)
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

When(
  'I delete note {string} and leave references as dead links',
  (noteTopology: string) => {
    start.jumpToNotePage(noteTopology).deleteNoteAndLeaveReferencesAsDeadLinks()
  }
)

When(
  'I delete note {string} and remove it from properties of references',
  (noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .deleteNoteAndRemoveFromReferenceProperties()
  }
)

When('I should see that the note creation is not successful', () => {
  start.form.getField('Title').expectError('must not be blank')
  cy.get('body').then(($body) => {
    const close = $body.find('.Vue-Toastification__close-button').get(0)
    if (close) {
      close.click()
    }
  })
})

Then(
  'I should see the note {string} is marked as deleted',
  (noteTopology: string) => {
    start.jumpToNotePage(noteTopology)
    cy.findByText('This note has been deleted')
  }
)

Then('I should be on a notebook folder page in the browser', () => {
  start.pageIsNotLoading()
  cy.location('pathname').should('match', /^\/notebooks\/\d+\/folders\/\d+$/)
})

Then('I should be on the notebook root page in the browser', () => {
  start.pageIsNotLoading()
  cy.location('pathname').should('match', /^\/notebooks\/\d+$/)
})

When('I navigate to {notepath} note', (notePath: NotePath) => {
  start.navigateToNoteFromPath(notePath)
})

When('I open note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle, true)
  start.pageIsNotLoading()
  start.assumeNotePage(noteTitle).switchToRichContent()
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
  cy.get('.daisy-btn[title^="undo"]').should('not.exist')
})

Then('I type {string} in the title', (content: string) => {
  cy.focused().clear().type(content)
})

Then(
  'the note content on the current page should be {string}',
  (contentText: string) => {
    start.assumeNotePage().findNoteContent(contentText)
  }
)

Then(
  'the note content on the current page should be {string} within {int} seconds',
  (contentText: string, timeout: number) => {
    start.assumeNotePage().findNoteContent(contentText, timeout * 1000)
  }
)

Given(
  'I request to complete the content for the note {string}',
  (noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .startAConversationAboutNote()
      .replyToConversationAndInviteAiToReply(
        'Please complete the note content.'
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

Then(
  'I should see the questions in the question list of the note {string}:',
  (_noteTopology: string, data: DataTable) => {
    start.assumeNotePage().expectQuestionsInList(data.hashes())
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
  'I upload an image from fixture {string} to the note {string}',
  (fixturePath: string, noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .switchToRichContentMode()
      .uploadRichNoteImagePropertyFromFixture(fixturePath)
  }
)

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

Then(
  'the rich note property {string} should show an attachment image path',
  (key: string) => {
    start.assumeNotePage().expectRichNoteImagePropertyAttachmentPath(key)
  }
)

Then('I should not see rich note property {string}', (key: string) => {
  start.assumeNotePage().expectRichNotePropertyAbsent(key)
})

Then('I should see wiki link {string} as a dead link', (linkText: string) => {
  start.assumeNotePage().expectDeadWikiLink(linkText)
})

When(
  'I edit the rich note property with key {string} to key {string} and value {string}',
  (oldKey: string, newKey: string, newValue: string) => {
    start.assumeNotePage().editRichNoteProperty(oldKey, newKey, newValue)
  }
)

When(
  'I update note {string} content using markdown to become:',
  (noteTopology: string, newContent: string) => {
    start.jumpToNotePage(noteTopology).updateContentAsMarkdown(newContent)
  }
)

When(
  'I update the current note content using markdown to become:',
  (newContent: string) => {
    start.assumeNotePage().updateContentAsMarkdown(newContent)
  }
)

When('I reload the current page for note {string}', (noteTopology: string) => {
  cy.reload()
  start.pageIsNotLoading()
  start.assumeNotePage(noteTopology)
})

When('I open the note content markdown editor', () => {
  start.assumeNotePage().openMarkdownContentEditor()
})

Then(
  'the note content markdown source should contain {string}',
  (fragment: string) => {
    start.assumeNotePage().expectMarkdownContentSourceContains(fragment)
  }
)

Then(
  'the note content markdown source should not contain {string}',
  (fragment: string) => {
    start.assumeNotePage().expectMarkdownContentSourceDoesNotContain(fragment)
  }
)

When('I view the note content as rich content', () => {
  start.assumeNotePage().switchToRichContentMode()
})

When('I view the note content as markdown', () => {
  start.assumeNotePage().toolbarButton('Edit as markdown').click()
})

Then(
  'I should see the rich content elements in the note content:',
  (data: DataTable) => {
    start.assumeNotePage().expectRichContent(data.hashes())
  }
)

Then(
  'I should see the rich content of the note with content:',
  (data: DataTable) => {
    start
      .assumeNotePage()
      .switchToRichContent()
      .expectRichContent(data.hashes())
  }
)

When(
  'I activate folder {string} in the sidebar and create a new note with title {string}',
  (folderLabel: string, title: string) => {
    start.jumpToNotePage('team')
    start
      .noteSidebar()
      .activateFolderByLabel(folderLabel)
      .addingNewNoteFromToolbar()
      .createNoteWithTitle(title)
    start.assumeNotePage(title)
    start.testability().rememberUiCreatedNote(title)
  }
)
Then(
  'I should see note {notepath} has content {string}',
  (notePath: NotePath, expectedContent: string) => {
    start
      .navigateToNotebooksPage()
      .navigateToPath(notePath)
      .findNoteContent(expectedContent)
  }
)

Then('the note content should contain a line break', () => {
  start.assumeNotePage().expectNoteContentContainLineBreak()
})

When(
  'I extract the suggestion {string} to a new note',
  (suggestionText: string) => {
    start.assumeAssimilationPage().extractSuggestionToNewNote(suggestionText)
  }
)

Then(
  'the link {string} should link to the note with the same title',
  (linkText: string) => {
    start
      .assumeNotePage()
      .wikiLinkInNoteContent(linkText)
      .expectNoteShowHref()
      .followAndAssumeNote(linkText)
  }
)

Then(
  'the link {string} should open the note titled {string}',
  (linkText: string, noteTitle: string) => {
    start
      .assumeNotePage()
      .wikiLinkInNoteContent(linkText)
      .expectNoteShowHref()
      .followAndAssumeNote(noteTitle)
  }
)

Then(
  'I should be able to create a new note by following the dead link {string}',
  (linkTitle: string) => {
    start.assumeNotePage().followDeadLink(linkTitle).createNote()
    start.testability().rememberUiCreatedNote(linkTitle)
    start.assumeNotePage(linkTitle)
  }
)

When(
  'I link dead link {string} to existing note {string}',
  (deadLinkText: string, existingNoteTitle: string) => {
    start
      .assumeNotePage()
      .followDeadLink(deadLinkText)
      .linkToExistingNote(existingNoteTitle, deadLinkText)
  }
)
