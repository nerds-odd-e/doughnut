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
  start.routerToNotebooksPage().shareNotebookToBazaar(noteTopic)
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
    start.routerToNotebooksPage().updateSubscription(noteTopic)
  }
)

When('I change notebook {string} to skip review', (noteTopic: string) => {
  start.routerToNotebooksPage().editNotebookSettings(noteTopic).skipReview()
})

When('I request for an approval for notebook {string}', (notebook: string) => {
  start
    .routerToNotebooksPage()
    .editNotebookSettings(notebook)
    .requestForNotebookApproval()
})

When(
  'the approval for the notebook {string} is {string}',
  (noteTopic: string, status: string) => {
    start
      .routerToNotebooksPage()
      .editNotebookSettings(noteTopic)
      .expectNotebookApprovalStatus(status)
  }
)

Then(
  'I should see the status {string} of the approval for notebook {string}',
  (status: string, noteTopic: string) => {
    start
      .routerToNotebooksPage()
      .editNotebookSettings(noteTopic)
      .expectNotebookApprovalStatus(status)
  }
)

Then(
  'I can request approval for the notebook {string} again',
  (noteTopic: string) => {
    start
      .routerToNotebooksPage()
      .editNotebookSettings(noteTopic)
      .expectNotebookApprovalCanBeRequested()
  }
)

Then('I unsubscribe from notebook {string}', (noteTopic: string) => {
  start.routerToNotebooksPage().unsubscribe(noteTopic)
})

Given(
  'I set the number of questions per assessment of the notebook {string} to {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .routerToNotebooksPage()
      .editNotebookSettings(notebook)
      .updateAssessmentSettings({ numberOfQuestion })
  }
)

Given(
  'the number of questions in assessment for notebook {string} is {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .routerToNotebooksPage()
      .editNotebookSettings(notebook)
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
      .openNotebookQuestions(notebook)
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
      .openNotebookQuestions(notebook)
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
      .editNotebookSettings(notebookName)
      .requestForNotebookApproval()
  })
})

Given('I answered an assessment question wrongly', () => {
  start
    .routerToNotebooksPage()
    .editNotebookSettings('Countries')
    .updateAssessmentSettings({ numberOfQuestion: 3 })
  start.navigateToBazaar().selfAssessmentOnNotebook('Countries')
  start
    .assumeAssessmentPage()
    .assumeQuestionSection()
    .answerWithoutContinuing('europe')
})

Then('I should get immediate feedback by showing the wrong answer', () => {
  cy.contains('europe').should('have.class', 'current-choice')
  cy.get('.current-choice').should('have.length', 1)
  cy.findByRole('button', { name: 'Continue' }).click()
  cy.get('.current-choice').should('have.length', 0)
})
