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

Given('I choose to share my notebook {string}', (noteTopology: string) => {
  start
    .routerToNotebooksPage()
    .notebookCard(noteTopology)
    .shareNotebookToBazaar()
})

Then(
  'I should see readonly notebook {string} in my notes',
  (noteTopology: string) => {
    start.routerToNotebooksPage()
    cy.findByText(noteTopology).click()
    cy.pageIsNotLoading()
    start.assumeNotePage().editNoteImage().shouldNotExist()
  }
)

Then(
  'I should be able to edit the subscription to notebook {string}',
  (noteTopology: string) => {
    start
      .routerToNotebooksPage()
      .notebookCard(noteTopology)
      .updateSubscription()
  }
)

When('I change notebook {string} to skip review', (noteTopology: string) => {
  start
    .routerToNotebooksPage()
    .notebookCard(noteTopology)
    .editNotebookSettings()
    .skipMemoryTracking()
})

When('I request for an approval for notebook {string}', (notebook: string) => {
  start
    .routerToNotebooksPage()
    .notebookCard(notebook)
    .editNotebookSettings()
    .requestForNotebookApproval()
})

When(
  'the approval for the notebook {string} is {string}',
  (noteTopology: string, status: string) => {
    start
      .routerToNotebooksPage()
      .notebookCard(noteTopology)
      .editNotebookSettings()
      .expectNotebookApprovalStatus(status)
  }
)

Then(
  'I should see the status {string} of the approval for notebook {string}',
  (status: string, noteTopology: string) => {
    start
      .routerToNotebooksPage()
      .notebookCard(noteTopology)
      .editNotebookSettings()
      .expectNotebookApprovalStatus(status)
  }
)

Then('I unsubscribe from notebook {string}', (noteTopology: string) => {
  start.routerToNotebooksPage().notebookCard(noteTopology).unsubscribe()
})

Given(
  'I set the number of questions per assessment of the notebook {string} to {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .routerToNotebooksPage()
      .notebookCard(notebook)
      .editNotebookSettings()
      .updateAssessmentSettings({ numberOfQuestion })
  }
)

Given(
  'the number of questions in assessment for notebook {string} is {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .routerToNotebooksPage()
      .notebookCard(notebook)
      .editNotebookSettings()
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
Then(
  'I should see that there are no questions for {string} for the following notes:',
  (notebook: string, titles: DataTable) => {
    const notebookQuestionsPage = start
      .routerToNotebooksPage()
      .notebookCard(notebook)
      .openNotebookQuestions()
    titles.rows().forEach((title: string[]) => {
      const titleName = title[0]!
      notebookQuestionsPage.expectNoQuestionsForNote(titleName)
    })
  }
)
Then(
  'I should see the following questions for the notes in the notebook {string}:',
  (notebook: string, noteTitles: DataTable) => {
    const notebookQuestionsPage = start
      .routerToNotebooksPage()
      .notebookCard(notebook)
      .openNotebookQuestions()
    noteTitles.rows().forEach((title: string[]) => {
      const titleName = title[0]!
      const question = title[1]!
      notebookQuestionsPage.expectOnlyQuestionsForNote(titleName, question)
    })
  }
)

Given('following notebooks have pending approval:', (notebooks: DataTable) => {
  notebooks.raw().forEach((notebookRaw: string[]) => {
    const notebookName = notebookRaw[0]!
    start
      .routerToNotebooksPage()
      .notebookCard(notebookName)
      .editNotebookSettings()
      .requestForNotebookApproval()
  })
})

Given(
  'in the assessment for notebook {string}, I wrongly answered the first assessment question with {string}',
  (notebook: string, answer: string) => {
    start
      .navigateToBazaar()
      .beginAssessmentOnNotebook(notebook)
      .assumeQuestionSection()
      .answerIncorrectly(answer)
  }
)

Then('I should get immediate feedback by showing the wrong answer', () => {
  start
    .assumeAssessmentPage()
    .assumeWrongAnswerPage()
    .highlightCurrentChoice('europe')
})

When(
  'I Import Obsidian data {string} to note {string}',
  (filename: string, noteTitle: string) => {
    start
      .routerToNotebooksPage()
      .notebookCard(noteTitle)
      .importObsidianData(filename)
  }
)

Then('I should see popup {string}', (message: string) => {
  // Wait for and verify alert message
  cy.on('window:alert', (text) => {
    expect(text).to.equal(message)
  })
})

When('I select {string} as default notebook', (notebookTitle: string) =>
  start.routerToNotebooksPage().notebookCard(notebookTitle).selectAsDefault()
)
