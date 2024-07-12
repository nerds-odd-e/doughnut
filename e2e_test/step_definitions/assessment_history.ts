import { DataTable, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then(
  'I must see the following assessments in my assessment history:',
  (dataTable: DataTable) => {
    start.navigateToAssessmentHistory()
    // look for exactly one row in a table and assert the content of each cell: assessment topic, correct answers, total questions
    const rows = dataTable.hashes()
    cy.get('table tbody tr').should('have.length', rows.length)
    rows.forEach((row, index) => {
      cy.get('table tbody tr')
        .eq(index)
        .within(() => {
          cy.findByText(row['notebook topic'] ?? '').should('exist')
          cy.findByText(row.score ?? '').should('exist')
          cy.findByText(row['total questions'] ?? '').should('exist')
        })
    })
  }
)
