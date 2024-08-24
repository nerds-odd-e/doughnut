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
      .answerYesNoQuestionsToScore(correctAnswers, allQuestions)
  }
)

When('I pass the assessment on {string}', (notebook: string) => {
  start.navigateToBazaar().selfAssessmentOnNotebook(notebook)
  start.assumeAssessmentPage(notebook).answerYesNoQuestionsToScore(2, 2)
})

Then('I cannot download a certificate after passing an assessment', () => {
  start.assumeAssessmentPage().expectCertificateCannotBeObtained()
})

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
  }
)

Given(
  'there is a notebook {string} by {string} with approved certifiable assessment',
  (notebook: string, creatorId: string) => {
    start
      .testability()
      .injectNumbersNotebookWithQuestions(notebook, 2, creatorId)
  }
)

Then('I should see my assessment history with empty records', () => {
  start.navigateToAssessmentHistory().expectTableWithNumberOfRow(0)
})

Then(
  'I should see {string} result as {string} in my assessment history',
  (notebook: string, result: string) => {
    start
      .navigateToAssessmentHistory()
      .expectTableWithNumberOfRow(1)
      .checkAttemptResult(notebook, result)
  }
)

When(
  'I should get a certificate of {string} for {string} from {string}',
  (notebook: string, user: string, creator: string) => {
    start
      .assumeAssessmentPage(notebook)
      .expectCertificate()
      .expectCertificateFor(notebook)
      .expectCertificateUser(user)
      .expectCertificateCreator(creator)
  }
)

Then(
  'I should not get a certificate of {string} for {string} from {string}',
  (notebook: string) => {
    start.assumeAssessmentPage(notebook).expectNoCertificate()
  }
)

When('Now is {string}', (dateString: string) => {
  start.testability().backendTimeTravelToDate(new Date(dateString))
})

When(
  'I should see the original start date {string} on my renewed certificate for {string}',
  (dateString: string, notebook: string) => {
    start
      .assumeAssessmentPage(notebook)
      .expectCertificate()
      .expectDate(dateString)
  }
)

Then(
  'I can view certificate of {string} in my assessment history',
  (notebook: string) => {
    start
      .navigateToAssessmentHistory()
      .expectCertificate(notebook)
      .expectCertificateFor(notebook)
  }
)

Then(
  'I can not view certificate of {string} in my assessment history',
  (notebook: string) => {
    start.navigateToAssessmentHistory().expectNoCertificate(notebook)
  }
)
