export const addEditQuestionPage = () => {
  return {
    fillQuestion(row: Record<string, string>) {
      ;[
        'Stem',
        'Choice 0',
        'Choice 1',
        'Choice 2',
        'Correct Choice Index',
      ].forEach((key: string) => {
        if (row[key] !== undefined && row[key] !== '') {
          cy.findByLabelText(key).clear().type(row[key]!)
        }
      })
    },

    editQuestion(question: string , row: Record<string, string>) {
      cy.findByText(question)
        .parent('tr')
        .contains('Edit')
        .click()
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Submit' }).click()
    },
    addQuestion(row: Record<string, string>) {
      cy.findByRole('button', {name: "Add Question"}).click()
      cy.findByRole('button', { name: '+' }).click()
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Submit' }).click()
    },
    generateQuestionByAI() {
      cy.findByRole('button', {name: "Add Question"}).click()
      cy.findByRole('button', { name: '+' }).click()
      cy.findByRole('button', { name: 'Generate by AI' }).click()
    },
    refineQuestion(row: Record<string, string>) {
      cy.findByRole('button', {name: "Add Question"}).click()
      cy.findByRole('button', { name: '+' }).click()
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Refine' }).click()
    },
  }
}
