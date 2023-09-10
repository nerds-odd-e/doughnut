export function adminDashboardPage() {
  return {
    downloadAIQuestionTrainingData() {
      cy.findByRole("button", { name: "Download" }).click()

      return {
        expectNumberOfRecords(count: number) {
          const downloadsFolder = Cypress.config("downloadsFolder")
          cy.readFile(`${downloadsFolder}/trainingdata.txt`)
            .then((content) => (content.match(/messages/g) || []).length)
            .should("eq", count)
        },
      }
    },
  }
}
