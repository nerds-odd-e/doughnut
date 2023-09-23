export function adminDashboardPage() {
  return {
    goToFailureReportList() {
      cy.findByText("Failure Reports").click()
      return {
        shouldContain(content: string) {
          cy.get("body").should("contain", content)
        },
      }
    },

    suggestedQuestionsForFineTuning() {
      cy.findByRole("button", { name: "Training Questions" }).click()
      return {
        expectComment(comment: string) {
          cy.findByText(comment)
        },
        downloadAIQuestionTrainingData() {
          cy.findByRole("button", { name: "Download" }).click()

          return {
            expectNumberOfRecords(count: number) {
              const downloadsFolder = Cypress.config("downloadsFolder")
              cy.readFile(`${downloadsFolder}/trainingdata.txt`)
                .then((content) => (content.match(/messages/g) || []).length)
                .should("eq", count)
              return this
            },

            expectTxtInDownload(inputText: string) {
              const downloadsFolder = Cypress.config("downloadsFolder")
              cy.readFile(`${downloadsFolder}/trainingdata.txt`).should("contain", inputText)
            },
          }
        },
      }
    },
  }
}
