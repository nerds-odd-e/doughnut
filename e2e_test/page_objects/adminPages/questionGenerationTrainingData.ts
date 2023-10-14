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
        cy.readFile(downloadFilename).then((content) => {
          if (question["Question Stem"] !== undefined) {
            expect(content).contain(question["Question Stem"])
          }
          if (question["Choices"] !== undefined) {
            question["Choices"].commonSenseSplit(",").forEach((choice: string) => {
              expect(content).contain(choice)
            })
          }
        })
      })
    },
  }
}
