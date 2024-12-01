/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import {
  Given,
  Then,
  When,
  DataTable,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I choose to share my notebook {string}', (noteTopic: string) => {
  start.routerToNotebooksPage().notebookCard(noteTopic).shareNotebookToBazaar()
})

Then(
  'I should see readonly notebook {string} in my notes',
  (noteTopic: string) => {
    start.routerToNotebooksPage()
    cy.findByText(noteTopic).click()
    cy.pageIsNotLoading()
    start.assumeNotePage().editNoteImage().shouldNotExist()
  }
)

Then(
  'I should be able to edit the subscription to notebook {string}',
  (noteTopic: string) => {
    start.routerToNotebooksPage().notebookCard(noteTopic).updateSubscription()
  }
)

When('I change notebook {string} to skip review', (noteTopic: string) => {
  start
    .routerToNotebooksPage()
    .notebookCard(noteTopic)
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
  (noteTopic: string, status: string) => {
    start
      .routerToNotebooksPage()
      .notebookCard(noteTopic)
      .editNotebookSettings()
      .expectNotebookApprovalStatus(status)
  }
)

Then(
  'I should see the status {string} of the approval for notebook {string}',
  (status: string, noteTopic: string) => {
    start
      .routerToNotebooksPage()
      .notebookCard(noteTopic)
      .editNotebookSettings()
      .expectNotebookApprovalStatus(status)
  }
)

Then('I unsubscribe from notebook {string}', (noteTopic: string) => {
  start.routerToNotebooksPage().notebookCard(noteTopic).unsubscribe()
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
  'I should see that there are no questions for {string} for the following topics:',
  (notebook: string, topics: DataTable) => {
    const notebookQuestionsPage = start
      .routerToNotebooksPage()
      .notebookCard(notebook)
      .openNotebookQuestions()
    topics.rows().forEach((topic: string[]) => {
      const topicName = topic[0]!
      notebookQuestionsPage.expectNoQuestionsForTopic(topicName)
    })
  }
)
Then(
  'I should see the following questions for the topics in the notebook {string}:',
  (notebook: string, topics: DataTable) => {
    const notebookQuestionsPage = start
      .routerToNotebooksPage()
      .notebookCard(notebook)
      .openNotebookQuestions()
    topics.rows().forEach((topic: string[]) => {
      const topicName = topic[0]!
      const question = topic[1]!
      notebookQuestionsPage.expectOnlyQuestionsForTopic(topicName, question)
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
