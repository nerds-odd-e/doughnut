import { Then, When, Given } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string_util"
import start from "../start"

When("I start the assessment on the {string} notebook in the bazaar", (notebook: string) => {
  start.navigateToBazaar().selfAssessmentOnNotebook(notebook)
})

Then("I answer the question {string} with {string}", function (stem: string, answer: string) {
  start.assumeAssessmentPage().expectQuestion(stem).answer(answer)
})

Then("I should see the score {string} at the end of assessment", (expectedScore: string) => {
  start.assumeAssessmentPage().expectEndOfAssessment(expectedScore)
})

Then("I should see error message Not enough questions", () => {
  cy.findByText("Not enough questions").should("be.visible")
})

Then("I should see error message The assessment is not available", () => {
  cy.findByText("The assessment is not available").should("be.visible")
})

Given("I want to create a question for the note {string}", (noteName: string) => {
  start.jumpToNotePage(noteName).goToAddQuestion()
})

Then("The {string} button should be disabled", (buttonName: string) => {
  cy.findByRole("button", { name: buttonName }).should("be.disabled")
})

Given("I fill {string} to the Stem of the question", (stem: string) => {
  start.assumeNotePage().injectDataToQuestion("Stem", stem)
})

Given("OpenAI now generates refine question:", (questionTable: DataTable) => {
  start.questionGenerationService().resetAndStubAskingMCQ(questionTable.hashes()[0])
})

Given("I fill {string} to the Choice 0 of the question", (choice: string) => {
  start.assumeNotePage().injectDataToQuestion("Choice 0", choice)
})

Given("I fill {string} to the Choice 1 of the question", (choice: string) => {
  start.assumeNotePage().injectDataToQuestion("Choice 1", choice)
})

Given("I fill {string} to the Correct Choice Index of the question", (correctIndex: string) => {
  start.assumeNotePage().injectDataToQuestion("Correct Choice Index", correctIndex)
})

When("I refine the question", () => {
  cy.findByRole("button", { name: "Refine" }).click()
})

Then("The refined question's {string} should not empty", (questionField: string) => {
  start.assumeNotePage().verifyRefineQuestionField(questionField)
})
