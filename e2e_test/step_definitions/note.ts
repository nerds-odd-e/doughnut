/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  DataTable,
  Given,
  Then,
  When,
  defineParameterType,
} from '@badeball/cypress-cucumber-preprocessor'
import NotePath from '../support/NotePath'
import '../support/string_util'
import start from '../start'

defineParameterType({
  name: 'notepath',
  regexp: /.*/,
  transformer(s: string) {
    return new NotePath(s)
  },
})

Given(
  'I have a notebook with head note {string} and notes:',
  (notebookTopic: string, data: DataTable) => {
    const notes = data.hashes()
    notes.unshift({ Topic: notebookTopic })
    start.testability().injectNotes(notes)
  }
)

Given('there are some notes:', (data: DataTable) => {
  data.hashes().forEach((note) => {
    if (!note['Parent Topic']) {
      throw new Error('Parent Topic is required for all notes')
    }
  })
  start.testability().injectNotes(data.hashes())
})

Given('I have a notebook with the head note {string}', (noteTopic: string) => {
  start.testability().injectNotes([{ Topic: noteTopic }])
})

Given(
  'I have a notebook with the head note {string} which skips review',
  (noteTopic: string) => {
    start.testability().injectNotes([{ Topic: noteTopic, 'Skip Review': true }])
  }
)

Given(
  'I have a notebook with the head note {string} and details {string}',
  (noteTopic: string, details: string) => {
    start.testability().injectNotes([{ Topic: noteTopic, Details: details }])
  }
)

Given(
  'there are some notes for existing user {string}',
  (externalIdentifier: string | undefined, data: DataTable) => {
    start.testability().injectNotes(data.hashes(), externalIdentifier)
  }
)

Given(
  'there are notes from Note {int} to Note {int}',
  (from: number, to: number) => {
    const notes = Array(to - from + 1)
      .fill(0)
      .map((_, i) => {
        return { Topic: `Note ${i + from}` }
      })
    start.testability().injectNotes(notes)
  }
)

Given('there are questions for the note:', (data: DataTable) => {
  start.testability().injectQuizQuestions(data.hashes())
})

Given(
  'I add the following question for the note {string}:',
  (noteTopic: string, data: DataTable) => {
    expect(data.hashes().length, 'please add one question at a time.').to.equal(
      1
    )
    start.jumpToNotePage(noteTopic).addQuestion(data.hashes()[0]!)
  }
)

When(
  'I edit the first questions first option to {string} in the question list of the note {string}:',
  (optionValue: string, noteTopic: string, data: DataTable) => {
    start.jumpToNotePage(noteTopic).editQuestion(data.hashes()[0]!, optionValue)
  }
)

When(
  'I delete the first question in the question list of the note {string}:',
  (noteTopic: string) => {
    start.jumpToNotePage(noteTopic).deleteFirstQuestion()
  }
)

Given(
  'I refine the following question for the note {string}:',
  (noteTopic: string, data: DataTable) => {
    expect(data.hashes().length, 'please add one question at a time.').to.equal(
      1
    )
    start.jumpToNotePage(noteTopic).refineQuestion(data.hashes()[0]!)
  }
)

When('I create a notebook with topic {string}', (notebookTopic: string) => {
  start.routerToNotebooksPage().creatingNotebook(notebookTopic)
})

When('I create a notebook with empty topic', () => {
  start.routerToNotebooksPage().creatingNotebook('')
})

When(
  'I update note {string} to become:',
  (noteTopic: string, data: DataTable) => {
    start.jumpToNotePage(noteTopic)
    cy.inPlaceEdit(data.hashes()[0])
  }
)

When(
  'I update note accessories of {string} to become:',
  (noteTopic: string, data: DataTable) => {
    start
      .jumpToNotePage(noteTopic)
      .updateNoteImage(data.hashes()[0]!)
      .updateNoteUrl(data.hashes()[0]!)
  }
)

When(
  'I upload an audio-file {string} to the note {string}',
  (fileName: string, noteTopic: string) => {
    start.jumpToNotePage(noteTopic)
    start.assumeNotePage().editAudioButton().click()
    cy.get('#note-uploadAudioFile').attachFile(fileName)
    cy.findAllByText('Save').click()
    cy.pageIsNotLoading()
  }
)

Then(
  'I must be able to download the {string} from the note {string}',
  (fileName: string, noteTopic: string) => {
    start.jumpToNotePage(noteTopic)
    start.assumeNotePage().downloadAudioFile(fileName)
  }
)

When('I convert the audio-file to SRT without saving', () => {
  cy.findAllByDisplayValue('Convert to SRT').click()
})

Then('I should see the extracted SRT content', (srtContent: string) => {
  cy.get('textarea').should('have.value', srtContent)
})

When(
  'I should see note {string} has a image and a url {string}',
  (noteTopic: string, expectedUrl: string) => {
    start.jumpToNotePage(noteTopic)
    cy.get('#note-image').should('exist')
    cy.findByLabelText('Url:').should('have.attr', 'href', expectedUrl)
  }
)

When(
  'I can change the topic {string} to {string}',
  (noteTopic: string, newNoteTopic: string) => {
    start.assumeNotePage(noteTopic)
    cy.inPlaceEdit({ topic: newNoteTopic })
    start.assumeNotePage(newNoteTopic)
  }
)

Given(
  'I update note topic {string} to become {string}',
  (noteTopic: string, newNoteTopic: string) => {
    start.jumpToNotePage(noteTopic)
    cy.inPlaceEdit({ topic: newNoteTopic })
  }
)

Given(
  'I update note {string} details from {string} to become {string}',
  (_noteTopic: string, noteDetails: string, newNoteDetails: string) => {
    cy.findByText(noteDetails).click({ force: true })
    cy.replaceFocusedTextAndEnter(newNoteDetails)
  }
)

When(
  'I update note {string} with details {string}',
  (noteTopic: string, newDetails: string) => {
    start.jumpToNotePage(noteTopic)
    cy.inPlaceEdit({ Details: newDetails })
    start.assumeNotePage().findNoteDetails(newDetails)
  }
)

When(
  'I create a note belonging to {string}:',
  (noteTopic: string, data: DataTable) => {
    expect(data.hashes().length).to.equal(1)
    start
      .jumpToNotePage(noteTopic)
      .addingChildNote()
      .createNoteWithAttributes(data.hashes()[0]!)
  }
)

When('I am creating a note under {notepath}', (notePath: NotePath) => {
  start.routerToNotebooksPage().navigateToPath(notePath).addingChildNote()
})

Then('I should see {string} in breadcrumb', (noteTopics: string) => {
  cy.pageIsNotLoading()
  cy.expectBreadcrumb(noteTopics)
})

When('I visit all my notebooks', () => {
  start.routerToNotebooksPage()
})

Then(
  'I should see these notes belonging to the user at the top level of all my notes',
  (data: DataTable) => {
    start.routerToNotebooksPage()
    cy.expectNoteCards(data.hashes())
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

When('I delete notebook {string}', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).deleteNote()
})

When(
  'I delete note {string} at {int}:00',
  (noteTopic: string, hour: number) => {
    start.testability().backendTimeTravelTo(0, hour)
    start.jumpToNotePage(noteTopic).deleteNote()
  }
)

When('I delete note {string}', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).deleteNote()
})

When('I should see that the note creation is not successful', () => {
  cy.expectFieldErrorMessage('Topic', 'size must be between 1 and 150')
  cy.dismissLastErrorMessage()
})

Then(
  'I should see the note {string} is marked as deleted',
  (noteTopic: string) => {
    start.jumpToNotePage(noteTopic)
    cy.findByText('This note has been deleted')
  }
)

Then(
  'I should not see note {string} at the top level of all my notes',
  (noteTopic: string) => {
    cy.pageIsNotLoading()
    cy.findByText('Notebooks')
    cy.get('main').within(() => cy.findCardTitle(noteTopic).should('not.exist'))
  }
)

When('I navigate to {notepath} note', (notePath: NotePath) => {
  start.routerToNotebooksPage().navigateToPath(notePath)
})

When('I click the child note {string}', (noteTopic: string) => {
  start.assumeNotePage().navigateToChild(noteTopic)
})

When('I move note {string} left', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic)
  cy.findByText('Move This Note').click()
  cy.findByRole('button', { name: 'Move Left' }).click()
})

When('I should see the screenshot matches', () => {
  // cy.get('.content').compareSnapshot('page-snapshot', 0.001);
})

When('I move note {string} right', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic)
  cy.findByText('Move This Note').click()
  cy.findByRole('button', { name: 'Move Right' }).click()
})

When(
  'I should see {string} is before {string} in {string}',
  (noteTopic1: string, noteTopic2: string, parentNoteTopic: string) => {
    start.jumpToNotePage(parentNoteTopic)
    const matcher = new RegExp(`${noteTopic1}.*${noteTopic2}`, 'g')

    cy.get('.card-title').then(($els) => {
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
  (noteTopic: string) => {
    cy.on('uncaught:exception', () => {
      return false
    })
    cy.get('main').within(() => {
      cy.findCardTitle(noteTopic).click()
    })
    cy.get('#username').should('exist')
  }
)

Then(
  'I should see {string} is {string} than {string}',
  (left: string, aging: string, right: string) => {
    let leftColor
    cy.pageIsNotLoading()
    start.jumpToNotePage(left)
    cy.get('.note-recent-update-indicator')
      .invoke('css', 'color')
      .then((val) => {
        leftColor = val
      })
    start.jumpToNotePage(right)
    cy.get('.note-recent-update-indicator')
      .invoke('css', 'color')
      .then((val) => {
        const leftColorIndex = parseInt(leftColor.match(/\d+/)[0])
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
  'the deleted notebook with topic {string} should be restored',
  (topic: string) => {
    start.assumeNotePage(topic)
  }
)

Then('there should be no more undo to do', () => {
  cy.get('.btn[title="undo"]').should('not.exist')
})

Then('I type {string} in the topic', (content: string) => {
  cy.focused().clear().type(content)
})

Then(
  'the note details on the current page should be {string}',
  (detailsText: string) => {
    start.assumeNotePage().findNoteDetails(detailsText)
  }
)

When('I generate an image for {string}', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).aiGenerateImage()
})

Then('I should find an art created by the ai', () => {
  cy.get('img.ai-art').should('be.visible')
})

Given(
  'I request to complete the details for the note {string}',
  (noteTopic: string) => {
    start.jumpToNotePage(noteTopic).aiSuggestDetailsForNote()
  }
)

Then(
  'I should see a notification of OpenAI service unavailability in the controller bar',
  () => {
    cy.get('.last-error-message')
      .should((elem) => {
        expect(elem.text()).to.equal('The OpenAI request was not Authorized.')
      })
      .click()
  }
)

When('I start to chat about the note {string}', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).chatAboutNote()
})

When(
  'I answer {string} to the clarifying question {string}',
  (answer: string, question: string) => {
    start.assumeClarifyingQuestionDialog(question).answer(answer)
  }
)

When(
  'I respond with "cancel" to the clarifying question {string}',
  (question: string) => {
    start.assumeClarifyingQuestionDialog(question).close()
  }
)

When('I should see a follow-up question {string}', (question: string) => {
  start.assumeClarifyingQuestionDialog(question)
  cy.wrap(question).as('lastClarifyingQuestion')
})

When(
  'the initial clarifying question with the response {string} should be visible',
  (oldAnswer: string) => {
    cy.get('@lastClarifyingQuestion').then((question) => {
      start
        .assumeClarifyingQuestionDialog(question as unknown as string)
        .oldAnswer(oldAnswer)
    })
  }
)

Then('I should see a child note {string}', (childTopic: string) => {
  start.assumeNotePage().expectChildren([{ 'note-topic': childTopic }])
})

When(
  'I try to upload an audio-file {string} to the note {string}',
  (fileName: string, noteTopic: string) => {
    start.jumpToNotePage(noteTopic)
    start.assumeNotePage().editAudioButton().click()
    cy.get('#note-uploadAudioFile').attachFile(fileName)
  }
)

When('I collapse the children of note {string}', (noteTopic: string) => {
  start.assumeNotePage(noteTopic).collapseChildren()
})

When('I expand the children of note {string}', (noteTopic: string) => {
  start.assumeNotePage(noteTopic).expandChildren()
})

When(
  'I expand the children of note {string} in the sidebar',
  (noteTopic: string) => {
    start.noteSidebar().expand(noteTopic)
  }
)

When(
  'I should see the note {string} with {int} children collapsed',
  (noteTopic: string, childrenCount: number) => {
    start.assumeNotePage(noteTopic).collapsedChildrenWithCount(childrenCount)
  }
)

Then('I should see the children notes:', (data: DataTable) => {
  cy.get('main').within(() => cy.expectNoteCards(data.hashes()))
})

When('I route to the note {string}', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic)
})

When(
  'I should see the questions in the question list of the note {string}:',
  (noteTopic: string, data: DataTable) => {
    start.jumpToNotePage(noteTopic).expectQuestionsInList(data.hashes())
  }
)

Given(
  'I toggle the approval of the question {string} of the topic {string}',
  (quizQuestion: string, noteTopic: string) => {
    start.jumpToNotePage(noteTopic).toggleApproval(quizQuestion)
  }
)

When('I generate question by AI for note {string}', (noteName: string) => {
  start
    .jumpToNotePage(noteName)
    .openQuestionList()
    .crudQuestionPage()
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
