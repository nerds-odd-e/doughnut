import { When, Then } from '@badeball/cypress-cucumber-preprocessor'

When('I export notebook {string} to GitHub', (notebookTitle: string) => {
  cy.findByText(notebookTitle)
    .closest('[data-cy="notebook-card"]')
    .findByRole('button', { name: /Export to GitHub/i })
    .click()
})

When('I input repository name {string}', (repoName: string) => {
  // Input repository name in the text field
  cy.get('[data-cy="repository-input"]')
    .type(repoName)
    .should('have.value', repoName)

  cy.findByRole('button', { name: 'Export' }).click()
})

Then('I should see a success message {string}', (message: string) => {
  cy.get('[data-testid="toast-message"]')
    .should('be.visible')
    .and('contain', message)
})
