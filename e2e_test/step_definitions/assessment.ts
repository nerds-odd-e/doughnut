import {
  DataTable,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import '../support/string_util'
import start from '../start'

When(
  'I start the assessment on the {string} notebook in the bazaar',
  (notebook: string) => {
    start.navigateToBazaar().selfAssessmentOnNotebook(notebook)
  }
)

When(
  'I do the assessment on {string} in the bazaar with the following answers:',
  function (notebook: string, table: DataTable) {
    start
      .navigateToBazaar()
      .selfAssessmentOnNotebook(notebook)
      .answerQuestionsFromTable(table.hashes())
  }
)

When(
  '{int} subsequent attempts of assessment on the {string} notebook should use {int} questions',
  (attempts: number, notebook: string, minUniqueQuestionsThreshold: number) => {
    const questions: string[] = []
    for (let i = 0; i < attempts; i++) {
      start.navigateToBazaar().selfAssessmentOnNotebook(notebook)
      const question = start.assumeAssessmentPage().assumeQuestionSection()
      question.getStemText().then((stem) => {
        questions.push(stem)
        question.answerFirstOption()
      })
    }
    cy.then(() => {
      const uniqueQuestions = new Set(questions)
      expect(uniqueQuestions.size).to.equal(minUniqueQuestionsThreshold)
    })
  }
)

Then(
  'I should see the score {string} at the end of assessment',
  (expectedScore: string) => {
    start.assumeAssessmentPage().expectEndOfAssessment(expectedScore)
  }
)

Then('I should see error message Not enough questions', () => {
  cy.findByText('Not enough questions').should('be.visible')
})

Then('I should see error message The assessment is not available', () => {
  cy.findByText('The assessment is not available').should('be.visible')
})

Given(
  'OpenAI now refines the question to become:',
  (questionTable: DataTable) => {
    start
      .questionGenerationService()
      .resetAndStubAskingMCQ(questionTable.hashes()[0]!)
  }
)

When(
  'I get score {int}\\/{int} when do the assessment on {string}',
  (correctAnswers: number, allQuestions: number, notebook: string) => {
    start
      .navigateToBazaar()
      .selfAssessmentOnNotebook(notebook)
      .answerYesNoQuestionsByScore(correctAnswers, allQuestions)
  }
)

Then('I should pass the assessment of {string}', (notebook: string) => {
  start.assumeAssessmentPage(notebook).passAssessment()
})

Then('I should not pass the assessment of {string}', (notebook: string) => {
  start.assumeAssessmentPage(notebook).expectNotPassAssessment()
})

Given(
  'there is an assessment on notebook {string} with {int} questions',
  (notebook: string, numberOfQuestion: number) => {
    start
      .testability()
      .injectNumbersNotebookWithQuestions(notebook, numberOfQuestion)
    start
      .routerToNotebooksPage()
      .updateAssessmentSettings(notebook, { numberOfQuestion })
  }
)

When('I view my assessment history', () => {
  start.navigateToAssessmentHistory()
})

Then('I should see my assessment history with empty records', () => {
  start
    .assumeViewAssessmentHistoryPage()
    .expectToFindTitle()
    .expectTableWithNumberOfRow(0)
})

Then('I should see one record of the assessment', () => {
  start
    .assumeViewAssessmentHistoryPage()
    .expectToFindTitle()
    .expectTableWithNumberOfRow(1)
})

Then(
  'I should see {string} result as {string}',
  (notebook: string, result: string) => {
    start
      .assumeViewAssessmentHistoryPage()
      .expectToFindTitle()
      .checkAttemptResult(notebook, result)
  }
)

Given(
  'I have completed the assessment on notebook {string} with {int} questions',
  (notebook: string, numberOfQuestion: number) => {
    start
      .testability()
      .injectNumbersNotebookWithQuestions(notebook, numberOfQuestion)
    start
      .routerToNotebooksPage()
      .updateAssessmentSettings(notebook, { numberOfQuestion })
  }
)
When(
  'I view my assessment history of topic {string} and scored {int}\\/{int} on the assessment',
  (notebook: string, correctAnswers: number, allQuestions: number) => {
    start
      .navigateToBazaar()
      .selfAssessmentOnNotebook(notebook)
      .answerYesNoQuestionsByScore(correctAnswers, allQuestions)

    start.navigateToAssessmentHistory()
  }
)
Then(
  "I should be able to click the 'Get Certificate' button to get my assessment certificate",
  () => {
    start.assumeViewAssessmentHistoryPage().expectEnabledCertificateButton()
  }
)
Then("I should not be able to click on the 'Get Certificate' button", () => {
  start.assumeViewAssessmentHistoryPage().expectDisabledCertificateButton()
})
When("I click on the 'Get Certificate' button", () => {
  start.assumeViewAssessmentHistoryPage().clickGetCertificate()
})
Then('I should be able to see my certificate', () => {
  start.assumeViewAssessmentHistoryPage().expectCertificateModal()
})
