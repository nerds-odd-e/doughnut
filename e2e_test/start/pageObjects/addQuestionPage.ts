export const addQuestionPage = () => {
  return {
    fillQuestionFields(row: Record<string, string>) {
      ;[
        'Stem',
        'Choice 0',
        'Choice 1',
        'Choice 2',
        'Correct Choice Index',
      ].forEach((key: string) => {
        if (row[key] !== undefined && row[key] !== '') {
          cy.findByLabelText(key)
            .clear()
            .invoke('val', row[key]!)
            .trigger('input')
        }
      })
    },
    fillQuestion(row: Record<string, string>) {
      cy.findByRole('button', { name: '+' }).click()
      this.fillQuestionFields(row)
    },
    addQuestion(row: Record<string, string>) {
      cy.intercept('POST', '**/api/predefined-questions/**/note-questions').as(
        'addQuestionManually'
      )
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Submit' }).click()
      cy.wait('@addQuestionManually').then(({ response }) => {
        expect(response?.statusCode, 'add question manually').to.equal(200)
      })
      cy.get('.question-table').should('contain.text', row.Stem!)
    },
    editQuestion(row: Record<string, string>) {
      cy.intercept(
        'PUT',
        '**/api/predefined-questions/**/note-questions/**'
      ).as('updateQuestion')
      // Edit mode is prefilled with the existing question's choices already,
      // so (unlike add) it must not click "+" to grow the choice list.
      this.fillQuestionFields(row)
      cy.findByRole('button', { name: 'Submit' }).click()
      cy.wait('@updateQuestion').then(({ response }) => {
        expect(response?.statusCode, 'update question').to.equal(200)
      })
      cy.get('.question-table').should('contain.text', row.Stem!)
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

export const deleteQuestion = (questionStem: string) => {
  cy.intercept('DELETE', '**/api/predefined-questions/**/note-questions/**').as(
    'deleteQuestion'
  )
  cy.contains('.question-table tr', questionStem).within(() => {
    cy.findByRole('button', { name: 'Delete question' }).click()
  })
  cy.findByRole('button', { name: 'OK' }).click()
  cy.wait('@deleteQuestion').then(({ response }) => {
    expect(response?.statusCode, 'delete question').to.equal(200)
  })
}
