import { When } from "@badeball/cypress-cucumber-preprocessor"
import start from "../start"

When("I go to the assessment history page", () => {
  start.navigateToAssessmentHistory()
})
