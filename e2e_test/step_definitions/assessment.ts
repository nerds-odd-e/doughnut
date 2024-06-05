import { When } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string_util"
import start from "../start"
import NotePath from "../support/NotePath"

When("I start the assessment on {notepath} notebook", (notePath: NotePath) => {
  start.routerToNotebooksPage().navigateToPath(notePath)

  cy.get("#dropdownMenuButton").click()

  cy.contains("Start Assessment").click()
})

When('I answer the question {string} with {string}', function (stem: string, answer: string) {
  cy.findByText(stem)
  cy.findByText(answer).click()
})
