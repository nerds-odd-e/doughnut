import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('I import notebook {string} from JSON file {string}', (notebook: string, filename: string) => {
  start.routerToNotebooksPage().notebookCard(notebook).importFromJson(filename)
})

When('I import all notebooks from JSON file {string}', (filename: string) => {
  start.routerToNotebooksPage().importAllFromJson(filename)
})

Then('I should see the imported notebook {string}', (notebook: string) => {
  cy.contains(notebook).should('exist')
})

Then('I should see the import success message', () => {
  cy.contains('Import completed successfully').should('exist')
})

Then('I should see the import error message', () => {
  cy.contains('Failed to import notebook(s)').should('exist')
}) 