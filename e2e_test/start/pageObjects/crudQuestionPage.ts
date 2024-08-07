export const crudQuestionPage = () => {
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
    editQuestion(row: Record<string, string>, questionOption: string) {
      cy.findByRole('button', { name: 'Edit Question' }).click()
      if (row['Choice 0'] !== undefined && row['Choice 0'] !== '') {
        cy.findByLabelText(row['Choice 0']).clear().type(questionOption)
      }
      cy.findByRole('button', { name: 'Submit' }).click()
    },
    deleteFirstQuestion() {
      cy.findAllByTitle('Delete Question').first().click()
    },
    addQuestion(row: Record<string, string>) {
      cy.findByRole('button', { name: 'Add Question' }).click({ force: true })
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Submit' }).click()
    },
    generateQuestionByAI() {
      cy.findByRole('button', { name: 'Add Question' }).click({ force: true })
      cy.findByRole('button', { name: 'Generate by AI' }).click()
    },
    refineQuestion(row: Record<string, string>) {
      cy.findByRole('button', { name: 'Add Question' }).click({ force: true })
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Refine' }).click()
    },
  }
}
