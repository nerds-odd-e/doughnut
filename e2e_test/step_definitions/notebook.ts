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
import { pageIsNotLoading } from '../start/pageBase'

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
      .pageIsNotLoading()
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

When('I request for an approval for notebook {string}', (notebook: string) => {
  start.navigateToNotebookPage(notebook).requestForNotebookApproval()
})

When(
  'the approval for the notebook {string} is {string}',
  (noteTopology: string, status: string) => {
    start
      .navigateToNotebookPage(noteTopology)
      .expectNotebookApprovalStatus(status)
  }
)

Then(
  'I should see the status {string} of the approval for notebook {string}',
  (status: string, noteTopology: string) => {
    start
      .navigateToNotebookPage(noteTopology)
      .expectNotebookApprovalStatus(status)
  }
)

Then('I unsubscribe from notebook {string}', (noteTopology: string) => {
  start.navigateToNotebooksPage().notebookCard(noteTopology).unsubscribe()
})

Given(
  'I set the number of questions per assessment of the notebook {string} to {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .navigateToNotebookPage(notebook)
      .updateAssessmentSettings({ numberOfQuestion })
  }
)

Given(
  'the number of questions in assessment for notebook {string} is {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .navigateToNotebookPage(notebook)
      .updateAssessmentSettings({ numberOfQuestion })
  }
)

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

Given('following notebooks have pending approval:', (notebooks: DataTable) => {
  notebooks.raw().forEach((notebookRaw: string[]) => {
    const notebookName = notebookRaw[0]!
    start.navigateToNotebookPage(notebookName).requestForNotebookApproval()
  })
})

function wrongAnswerChoiceForAssessmentStem(stem: string): string {
  const normalized = stem.replace(/\s+/g, ' ').trim()
  const byStem: Record<string, string> = {
    'Where in the world is Singapore?': 'europe',
    'Most famous food of Vietnam?': 'bread',
    'What is the capital city of Japan?': 'kyoto',
  }
  const wrong = byStem[normalized]
  expect(
    wrong,
    `No wrong-choice mapping for assessment stem: ${JSON.stringify(normalized)}`
  ).to.be.a('string')
  return wrong!
}

Given(
  'in the assessment for notebook {string}, I wrongly answered the first assessment question',
  (notebook: string) => {
    const assessmentPage = start
      .navigateToBazaar()
      .beginAssessmentOnNotebook(notebook)
    const questionSection = assessmentPage.assumeQuestionSection()
    questionSection.getStemText().then((stem) => {
      const wrong = wrongAnswerChoiceForAssessmentStem(stem)
      cy.wrap(wrong).as('wrongAnswerForFeedback')
      questionSection.answerIncorrectly(wrong)
    })
  }
)

Then('I should get immediate feedback by showing the wrong answer', () => {
  cy.get<string>('@wrongAnswerForFeedback').then((wrongAnswer) => {
    start
      .assumeAssessmentPage()
      .assumeWrongAnswerPage()
      .highlightCurrentChoice(wrongAnswer)
  })
})

When(
  'I Import Obsidian data {string} to note {string}',
  (filename: string, noteTitle: string) => {
    start.navigateToNotebookPage(noteTitle).importObsidianData(filename)
  }
)

When(
  'I open the notebook {string} from my notebooks catalog',
  (notebookName: string) => {
    start.navigateToNotebookPage(notebookName)
  }
)

When('I add the first note from the empty notebook page', () => {
  notebookPage().addFirstRootNoteFromEmptyNotebookPage()
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

When(
  'I rename the notebook from the notebook page summary to {string}',
  (newName: string) => {
    cy.get('[data-testid="notebook-page-title-edit"]').click()
    cy.get('[data-testid="notebook-page-name-input"]').clear().type(newName)
    cy.get('[data-testid="notebook-page-name-update"]').click()
    cy.findByRole('button', { name: 'OK' }).click()
    pageIsNotLoading()
  }
)
