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
  start.routerToNotebooksPage().skipReview(noteTopic)
})

When('I request for an approval for notebooks:', (notebooks: DataTable) => {
  notebooks.raw().forEach((notebookRaw: string[]) => {
    const notebookName = notebookRaw[0]!
    start.routerToNotebooksPage().requestForNotebookApproval(notebookName)
  })
})

When(
  'I cannot request approval again for notebook {string}',
  (noteTopic: string) => {
    start
      .routerToNotebooksPage()
      .expectNotebookApprovalCannotBeRequested(noteTopic)
  }
)

When(
  'the approval for the notebook {string} is {string}',
  (noteTopic: string, status: string) => {
    start
      .routerToNotebooksPage()
      .expectNotebookApprovalStatus(noteTopic, status)
  }
)

Then(
  'I should see the status {string} of the approval for notebook {string}',
  (status: string, noteTopic: string) => {
    start
      .routerToNotebooksPage()
      .expectNotebookApprovalStatus(noteTopic, status)
  }
)

Then(
  'I can request approval for the notebook {string}',
  (noteTopic: string) => {
    start
      .routerToNotebooksPage()
      .expectNotebookApprovalCanBeRequested(noteTopic)
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
      .updateAssessmentSettings(notebook, { numberOfQuestion })
  }
)

Given(
  'the number of questions in assessment for notebook {string} is {int}',
  (notebook: string, numberOfQuestion: number) => {
    start
      .routerToNotebooksPage()
      .updateAssessmentSettings(notebook, { numberOfQuestion })
  }
)

When(
  'I have done the assessment of the notebook {string} {int} times',
  (notebook: string, numberOfAttempts: number) => {
    const table: DataTable = new DataTable([
      ['Question', 'Answer'],
      ['Where in the world is Singapore?', 'Asia'],
    ])
    Cypress._.times(numberOfAttempts, () => {
      start
        .navigateToBazaar()
        .selfAssessmentOnNotebook(notebook)
        .answerQuestionsFromTable(table.hashes())
    })
  }
)

Then(
  'I should not be able to do assessment of the notebook {string} any more today',
  (notebook: string) => {
    start
      .navigateToBazaar()
      .selfAssessmentOnNotebook(notebook)
      .expectReachedLimit()
  }
)

Then(
  'I should be able to do assessment of the notebook {string} again the next day',
  (notebook: string) => {
    start
      .testability()
      .backendTimeTravelRelativeToNow(25)
      .then(() => {
        start
          .navigateToBazaar()
          .selfAssessmentOnNotebook(notebook)
          .expectQuestion('Where in the world is Singapore?')
      })
  }
)

Then(
  'I should see following notebooks waiting for approval:',
  (datatable: DataTable) => {
    for (let i = 0; i < datatable.rows().length; i++) {
      // const row = datatable.rows()[i]
      // const notebook = row[0]
    }
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
