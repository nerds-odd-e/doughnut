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
      cy.readFile(downloadFilename).then((content) => {
        const jsonStrings: string[] = content.split("\n")
        expect(jsonStrings.length).eq(questions.length)
        jsonStrings.forEach((line: string, index: number) => {
          const question: Record<string, string> = questions[index]
          expect(line).to.contain(question["Question Stem"])
          question["Choices"].split(", ").forEach((choice: string) => {
            expect(line).to.contain(choice)
          })
        })
      })
    },
  }
}
