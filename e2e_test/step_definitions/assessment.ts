import {
  DataTable,
  Given,
  Then,
  When,
} from "@badeball/cypress-cucumber-preprocessor"
import "../support/string_util"
import start from "../start"

When(
  "I start the assessment on the {string} notebook in the bazaar",
  (notebook: string) => {
    start.navigateToBazaar().selfAssessmentOnNotebook(notebook)
  },
)

Then(
  "I answer the question {string} with {string}",
  function (stem: string, answer: string) {
    start.assumeAssessmentPage().expectQuestion(stem).answer(answer)
  },
)

When('I answer with the {int} following answers:',
  function (expectedNumberOfQuestions: number, table: DataTable) {
    for (let i = 0; i < expectedNumberOfQuestions; i++) {
      start.assumeAssessmentPage().expectAQuestion().getStem().then(stem => {
        const row = table.hashes().find(row => row.question === stem)
        start.assumeAssessmentPage().expectQuestion(stem).answer(row.answer)
      })
    }
})

When(
  "{int} subsequent attempts of assessment on the {string} notebook should not have the same questions each time",
  (attempts: number, notebook: string) => {
    const questions: string[] = []
    for (let i = 0; i < attempts; i++) {
      start.navigateToBazaar().selfAssessmentOnNotebook(notebook)
      const question = start.assumeAssessmentPage().expectAQuestion()
      question.getStem().then((stem) => {
        questions.push(stem)
        question.answerFirst()
      })
    }
    cy.then(() => {
      const uniqueQuestions = new Set(questions)
      expect(uniqueQuestions.size).to.be.greaterThan(1)
    })
  },
)

Then(
  "I should see the score {string} at the end of assessment",
  (expectedScore: string) => {
    start.assumeAssessmentPage().expectEndOfAssessment(expectedScore)
  },
)

Then("I should see error message Not enough questions", () => {
  cy.findByText("Not enough questions").should("be.visible")
})

Then("I should see error message The assessment is not available", () => {
  cy.findByText("The assessment is not available").should("be.visible")
})

Then("I should see a link to the {string} notebook",
  (noteName: string) => {
    start.assumeAssessmentResultPage().expectCardFor(noteName)
    cy
      .findByRole("heading", { name: `Improve your knowledge by studying these notes` })
      .get('.card-body').click()
    start.assumeNotePage(noteName)
  }
)

Given(
  "OpenAI now refines the question to become:",
  (questionTable: DataTable) => {
    start
      .questionGenerationService()
      .resetAndStubAskingMCQ(questionTable.hashes()[0]!)
  },
)
