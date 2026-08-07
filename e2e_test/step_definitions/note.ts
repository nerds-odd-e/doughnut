/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Then,
  When,
  defineParameterType,
} from '@badeball/cypress-cucumber-preprocessor'
import NotePath from '../support/NotePath'
import '../support/string_util'
import start from '../start'
import {
  expectNoteAppearsAsRecentAs,
  expectNoteAppearsNewerThan,
} from '../start/pageObjects/noteRecentUpdate'

defineParameterType({
  name: 'notepath',
  regexp: /.*/,
  transformer(s: string) {
    return new NotePath(s)
  },
})

When(
  'I add the following question for the note {string}:',
  (noteTopology: string, data: DataTable) => {
    expect(data.hashes().length, 'please add one question at a time.').to.equal(
      1
    )
    start.jumpToNotePage(noteTopology).addQuestion(data.hashes()[0]!)
  }
)

When(
  'I refine the following question for the note {string}:',
  (noteTopology: string, data: DataTable) => {
    expect(data.hashes().length, 'please add one question at a time.').to.equal(
      1
    )
    start.jumpToNotePage(noteTopology, true).refineQuestion(data.hashes()[0]!)
  }
)

When('I type {string} in the title', (content: string) => {
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

When(
  'I request to complete the content for the note {string}',
  (noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .startAConversationAboutNote()
      .replyToConversationAndInviteAiToReply(
        'Please complete the note content.'
      )
    start.waitUntilAppIsNotBusy()
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

When('I generate a question with AI for note {string}', (noteName: string) => {
  start
    .jumpToNotePage(noteName, true)
    .openQuestionList()
    .addQuestionPage()
    .generateQuestionWithAI()
})

Then('the question in the form becomes:', (data: DataTable) => {
  start.assumeNotePage().expectQuestionInForm(data.hashes()[0]!)
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
  'I should see that {string} is newer than {string}',
  (left: string, right: string) => {
    expectNoteAppearsNewerThan(left, right)
  }
)

Then(
  'I should see that {string} is as recent as {string}',
  (left: string, right: string) => {
    expectNoteAppearsAsRecentAs(left, right)
  }
)
