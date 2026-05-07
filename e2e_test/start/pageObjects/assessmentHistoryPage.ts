export const assumeAssessmentHistoryPage = () => {
  cy.get('h2.fs-4').contains('My Assessment History').should('be.visible')

  const findNotebookCell = (notebook: string, column: number) => {
    return cy
      .get('.assessment-table tbody')
      .contains('td', notebook)
      .siblings('td')
      .eq(column)
  }

  return {
    expectTableWithNumberOfRow(n: number) {
      cy.get('.assessment-table tbody tr').should('have.length', n)
      return this
    },
    checkAttemptResult(notebook: string, result: string) {
      findNotebookCell(notebook, 1).contains(result)
    },
  }
}
