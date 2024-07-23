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
  'I get {int} percent score when do the assessment on {string}',
  (score: number, notebook: string) => {
    start
      .navigateToBazaar()
      .selfAssessmentOnNotebook(notebook)
      .answerQuestionsByScore(score)
  }
)

Then(
  'I should receive a certificate of {string} certified by {string}',
  (notebook: string, certifiedBy: string) => {
    start.assumeAssessmentPage(notebook).getCertificate(notebook, certifiedBy)
  }
)

Then(
  'I should not receive a certificate of {string} certified by {string}',
  (notebook: string, _certifiedBy: string) => {
    start.assumeAssessmentPage(notebook).expectNotPassAssessment()
  }
)

Given(
  'there is an assessment on notebook {string} with {int} questions certified by {string}',
  (notebook: string, numberOfQuestion: number, certifiedBy: string) => {
    start.testability().injectNumberNotes(notebook, numberOfQuestion)
    start.testability().injectYesNoQuestionsForNumberNotes(numberOfQuestion)
    start
      .routerToNotebooksPage()
      .updateAssessmentSettings(notebook, numberOfQuestion, certifiedBy)
    start.testability().shareToBazaar(notebook)
  }
)

When(
  'I finish the assessment for the notebook {string} with score {int}',
  (notebook: string, score: number) => {
    start
      .navigateToBazaar()
      .selfAssessmentOnNotebook(notebook)
      .answerQuestionsByScore(score)
  }
)

Then(
  'I should receive my {string} certificate with the issue date today and expiring on {string}',
  (notebook: string, expiredDate: string) => {
    start.assumeAssessmentPage(notebook).getExpiredDate(expiredDate)
  }
)

Then(
  'I should receive my certification of the {string} with a {string}',
  (notebook: string, newExpirationDate: string) => {
    start
      .assumeAssessmentPage(notebook)
      .getCertificate(notebook, undefined, newExpirationDate)
  }
)
