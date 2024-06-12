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

Then("I should see error message Not enough approved questions", () => {
  cy.findByText("Not enough approved questions").should("be.visible")
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
