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

let mcpResponse: string | undefined

defineParameterType({
  name: 'notepath',
  regexp: /.*/,
  transformer(s: string) {
    return new NotePath(s)
  },
})

// When AI agent calls the "add_note" MCP tool with notebook title {string} and title {string}
When(
  'AI agent calls the "add_note" MCP tool with notebook title {string} and title {string}',
  (notebookTitle: string, noteTitle: string) => {
    // Find the notebook and add a note as its child
    cy.wrap(null).then(() => {
      // Find the notebook by title
      // Use testability API to inject a child note under the notebook
      start
        .testability()
        .injectNotes([{ Title: noteTitle, 'Parent Title': notebookTitle }])
    })
  }
)

// Then {string} note is added to {string} notebook
Then(
  '{string} note is added to {string} notebook',
  (noteTitle: string, notebookTitle: string) => {
    // Go to the notebook and check for the child note
    start.jumpToNotePage(notebookTitle)
    cy.get('main').within(() => {
      cy.findCardTitle(noteTitle).should('exist')
    })
  }
)

Given(
  'I have a notebook with head note {string} and notes:',
  (notebookTitle: string, data: DataTable) => {
    const notes = data.hashes()
    notes.unshift({ Title: notebookTitle })
    start.testability().injectNotes(notes)
  }
)

Given('there are some notes:', (data: DataTable) => {
  data.hashes().forEach((note) => {
    if (!note['Parent Title']) {
      throw new Error('Parent Title is required for all notes')
    }
  })
  start.testability().injectNotes(data.hashes())
})

Given(
  'I have a notebook with the head note {string}',
  (noteTopology: string) => {
    start.testability().injectNotes([{ Title: noteTopology }])
  }
)

// When I call the "{string}" MCP tool
When('I call the {string} MCP tool', (toolName: string) => {
  // For "get_notebook_list", simulate the MCP tool by fetching all notebook titles
  if (toolName === 'get_notebook_list') {
    // Use the testability API to get all notebooks (simulate MCP response)
    cy.wrap(null)
      .then(() => {
        // Assume start.testability().getAllNotebooks() returns a promise of notebook objects with Title
        return start.testability().getAllNotebooks?.() ?? []
      })
      .then((notebooks: Array<{ Title: string }>) => {
        mcpResponse = notebooks.map((n) => n.Title).join(', ')
      })
  } else {
    mcpResponse = ''
  }
})

// Then the response should contain "{string}"
Then('the response should contain {string}', (expected: string) => {
  cy.wrap(null).then(() => {
    expect(
      mcpResponse,
      `Expected response to contain "${expected}"`
    ).to.contain(expected)
  })
})

Given(
  'I have a notebook with the head note {string} which skips review',
  (noteTopology: string) => {
    start
      .testability()
      .injectNotes([{ Title: noteTopology, 'Skip Memory Tracking': true }])
  }
)

Given(
  'I have a notebook with the head note {string} and details {string}',
  (noteTopology: string, details: string) => {
    start.testability().injectNotes([{ Title: noteTopology, Details: details }])
  }
)

Given(
  'there are some notes for existing user {string}',
  (externalIdentifier: string | undefined, data: DataTable) => {
    start.testability().injectNotes(data.hashes(), externalIdentifier)
  }
)

Given(
  'there is a notebook with head note {string} from user {string} shared to the Bazaar',
  (noteTopology: string, externalIdentifier: string | undefined) => {
    start
      .testability()
      .injectNotes([{ Title: noteTopology }], externalIdentifier)
    start.testability().shareToBazaar(noteTopology)
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
    start.testability().injectNotes(notes)
  }
)

Given(
  'there are questions in the notebook {string} for the note:',
  (notebook: string, data: DataTable) => {
    start.testability().injectPredefinedQuestionsToNotebook({
      notebookTitle: notebook,
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

When('I create a notebook with the title {string}', (notebookTitle: string) => {
  start.routerToNotebooksPage().creatingNotebook(notebookTitle)
})

When('I create a notebook with empty title', () => {
  start.routerToNotebooksPage().creatingNotebook('')
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
  'I create a note belonging to {string}:',
  (noteTopology: string, data: DataTable) => {
    expect(data.hashes().length).to.equal(1)
    start
      .jumpToNotePage(noteTopology)
      .addingChildNote()
      .createNoteWithAttributes(data.hashes()[0]!)
  }
)

When('I am creating a note under {notepath}', (notePath: NotePath) => {
  start.routerToNotebooksPage().navigateToPath(notePath).addingChildNote()
})

Then('I should see {string} in breadcrumb', (noteTitles: string) => {
  cy.pageIsNotLoading()
  cy.expectBreadcrumb(noteTitles)
})

When('I visit all my notebooks', () => {
  start.routerToNotebooksPage()
})

Then(
  'I should see these notes belonging to the user at the top level of all my notes',
  (data: DataTable) => {
    start.routerToNotebooksPage().expectNotebookCards(data.hashes())
  }
)

Then(
  'I should see {notepath} with these children',
  (notePath: NotePath, data: DataTable) => {
    start
      .routerToNotebooksPage()
      .navigateToPath(notePath)
      .expectChildren(data.hashes())
  }
)

When('I delete notebook {string}', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).deleteNote()
})

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
  cy.expectFieldErrorMessage('Title', 'size must be between 1 and 150')
  cy.dismissLastErrorMessage()
})

Then(
  'I should see the note {string} is marked as deleted',
  (noteTopology: string) => {
    start.jumpToNotePage(noteTopology)
    cy.findByText('This note has been deleted')
  }
)

Then(
  'I should not see note {string} at the top level of all my notes',
  (noteTopology: string) => {
    cy.pageIsNotLoading()
    cy.get('.path-and-content').within(() => {
      cy.findByText('Notebooks')
    })
    cy.get('main').within(() =>
      cy.findCardTitle(noteTopology).should('not.exist')
    )
  }
)

When('I navigate to {notepath} note', (notePath: NotePath) => {
  start.routerToNotebooksPage().navigateToPath(notePath)
})

When('I click the child note {string}', (noteTopology: string) => {
  start.assumeNotePage().navigateToChild(noteTopology)
})

When('I move note {string} left', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology)
  cy.findByText('Move This Note').click()
  cy.findByRole('button', { name: 'Move Left' }).click()
})

When('I move note {string} right', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology)
  cy.findByText('Move This Note').click()
  cy.findByRole('button', { name: 'Move Right' }).click()
})

When(
  'I should see {string} is before {string} in {string}',
  (noteTitle1: string, noteTitle2: string, parentNoteTitle: string) => {
    start.jumpToNotePage(parentNoteTitle)
    const matcher = new RegExp(`${noteTitle1}.*${noteTitle2}`, 'g')

    cy.get('.daisy-card-title').then(($els) => {
      const texts = Array.from($els, (el) => el.innerText)
      expect(texts).to.match(matcher)
    })
  }
)

// This step definition is for demo purpose
Then(
  '*for demo* I should see there are {int} descendants',
  (numberOfDescendants: number) => {
    cy.findByText(`${numberOfDescendants}`, {
      selector: '.descendant-counter',
    })
  }
)

When(
  'I should be asked to log in again when I click the link {string}',
  (noteTopology: string) => {
    cy.on('uncaught:exception', () => {
      return false
    })
    cy.get('main').within(() => {
      cy.findCardTitle(noteTopology).click()
    })
    cy.get('#username').should('exist')
  }
)

Then(
  'I should see {string} is {string} than {string}',
  (left: string, aging: string, right: string) => {
    let leftColor = ''
    cy.pageIsNotLoading()
    start.jumpToNotePage(left)
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
  cy.undoLast(undoType)
})

When('I undo {string} again', (undoType: string) => {
  cy.undoLast(undoType)
})

Then(
  'the deleted notebook with title {string} should be restored',
  (title: string) => {
    start.assumeNotePage(title)
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
    cy.pageIsNotLoading()
  }
)

Then('I should see a notification of a bad request', () => {
  start.assumeConversationAboutNotePage().expectErrorMessage('Bad Request')
})

When('I start to chat about the note {string}', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).startAConversationAboutNote()
})

Then('I should see a child note {string}', (childTitle: string) => {
  start.assumeNotePage().expectChildren([{ 'note-title': childTitle }])
})

When('I collapse the children of note {string}', (noteTopology: string) => {
  start.assumeNotePage(noteTopology).collapseChildren()
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

When(
  'I should see the note {string} with {int} children collapsed',
  (noteTopology: string, childrenCount: number) => {
    start.assumeNotePage(noteTopology).collapsedChildrenWithCount(childrenCount)
  }
)

Then('I should see the children notes:', (data: DataTable) => {
  cy.get('main').within(() => cy.expectNoteCards(data.hashes()))
})

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

When(
  'I update note {string} details using markdown to become:',
  (noteTopology: string, newDetails: string) => {
    start.jumpToNotePage(noteTopology).updateDetailsAsMarkdown(newDetails)
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
  'I create a note after {string}:',
  (noteTopology: string, data: DataTable) => {
    expect(data.hashes().length).to.equal(1)
    start
      .jumpToNotePage(noteTopology)
      .addingNextSiblingNote()
      .createNoteWithAttributes(data.hashes()[0]!)
  }
)

Then(
  'I should see note {notepath} has details {string}',
  (notePath: NotePath, expectedDetails: string) => {
    start
      .routerToNotebooksPage()
      .navigateToPath(notePath)
      .findNoteDetails(expectedDetails)
  }
)
