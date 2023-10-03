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
      cy.findByRole("button", { name: "Fine Tuning Data" }).click()
      return {
        expectComment(comment: string) {
          cy.findByText(comment)
        },
        downloadAIQuestionTrainingData() {
          cy.findByRole("button", { name: "Download" }).click()
          const downloadsFolder = Cypress.config("downloadsFolder")

          return {
            expectNumberOfRecords(count: number) {
              cy.readFile(`${downloadsFolder}/trainingdata.txt`)
                .then((content) => (content.match(/messages/g) || []).length)
                .should("eq", count)
              return this
            },

            expectTxtInDownload(inputText: string) {
              cy.readFile(`${downloadsFolder}/trainingdata.txt`).should("contain", inputText)
            },
          }
        },
      }
    },
  }
}
