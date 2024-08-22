export const addQuestionPage = () => {
  return {
    fillQuestion(row: Record<string, string>) {
      cy.findByRole('button', { name: '+' }).click()
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
    addQuestion(row: Record<string, string>) {
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Submit' }).click()
    },
    generateQuestionByAI() {
      cy.findByRole('button', { name: 'Generate by AI' }).click()
    },
    refineQuestion(row: Record<string, string>) {
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Refine' }).click()
    },
  }
}
