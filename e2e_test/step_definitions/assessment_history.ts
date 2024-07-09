import { DataTable, Then, When } from "@badeball/cypress-cucumber-preprocessor"
import start from "../start"

When("I go to the assessment history page", () => {
  start.navigateToAssessmentHistory()
})

Then("I should see an empty assessment list", () => {
  cy.findByText("No assessment has been done yet")
})

Then("I see the following assessments:", (dataTable: DataTable) => {
  // look for exactly one row in a table and assert the content of each cell: assessment topic, correct answers, total questions
  const rows = dataTable.hashes()
  cy.get("table tbody tr").should("have.length", rows.length)
  rows.forEach((row, index) => {
    cy.get("table tbody tr")
      .eq(index)
      .within(() => {
        cy.findByText(row["notebook topic"] ?? "").should("exist")
        cy.findByText(row.score ?? "").should("exist")
        cy.findByText(row["total questions"] ?? "").should("exist")
      })
  })
})
