import { Then, When } from "@badeball/cypress-cucumber-preprocessor"
import start from "../start"

When("I go to the assessment history page", () => {
  start.navigateToAssessmentHistory()
})

Then("I should see an empty assessment list", () => {
  cy.findByText("No assessment has been done yet")
})
