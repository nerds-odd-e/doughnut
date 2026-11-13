/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
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
import { fetchOpenAiMockIsolationProof } from './worktreeOpenAiMockIsolation'

defineParameterType({
  name: 'notepath',
  regexp: /.*/,
  transformer(s: string) {
    return new NotePath(s)
  },
})

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

Then(
  'the note content on the current page should be either {string} or {string} within {int} seconds',
  (first: string, second: string, timeout: number) => {
    start.assumeNotePage().findNoteContentEither(first, second, timeout * 1000)
  }
)

When(
  'I request to complete the content for the note {string}',
  (noteTopology: string) => {
    fetchOpenAiMockIsolationProof().then((proof) => {
      const message = proof
        ? `Please complete the note content. ${proof.requestMarker}`
        : 'Please complete the note content.'
      start
        .jumpToNotePage(noteTopology)
        .startAConversationAboutNote()
        .replyToConversationAndInviteAiToReply(message)
    })
  }
)

Then('I should see a notification of a bad request', () => {
  start.assumeConversationAboutNotePage().expectErrorMessage('Bad Request')
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
