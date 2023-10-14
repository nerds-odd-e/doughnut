export function quesionGenerationTrainingData() {
  const downloadFilename = `${Cypress.config("downloadsFolder")}/fineTuningData.jsonl`

  return {
    expectNumberOfRecords(count: number) {
      cy.readFile(downloadFilename)
        .then((content) => (content.match(/messages/g) || []).length)
        .should("eq", count)
      return this
    },

    expectExampleQuestions(questions: Record<string, string>[]) {
      questions.forEach((question: Record<string, string>) => {
        if (question["Question Stem"] !== undefined) {
          cy.readFile(downloadFilename).should("contain", question["Question Stem"])
        }
        if (question["Choices"] !== undefined) {
          question["Choices"].commonSenseSplit(",").forEach((choice: string) => {
            cy.readFile(downloadFilename).should("contain", choice)
          })
        }
      })
    },
  }
}
