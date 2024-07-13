export const editQuestionPage = (question: string) => {
  cy.contains('td', question)
    .parent('tr')
    .children('td')
    .findByRole('button', { name: 'Edit Question' })
    .click()
  cy.get('.modal-container').should('be.visible')
  return {
    fillQuestion(row: Record<string, string>) {
      ;['Correct Choice Index'].forEach((key: string) => {
        if (row[key] !== undefined && row[key] !== '') {
          cy.findByLabelText(key).type(row[key]!)
        }
      })
    },
    editRow(row: Record<string, string>) {
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Submit' }).click()
    },
  }
}
