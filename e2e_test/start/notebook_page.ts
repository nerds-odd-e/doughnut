importObsidianData(filename: string) {
  cy.get('input[type="file"]').selectFile(
    `e2e_test/fixtures/${filename}`,
    { force: true }
  )
  cy.get('.daisy-alert-success').should('be.visible')
  return this
} 