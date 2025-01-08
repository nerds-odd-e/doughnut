import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('I export notebook {string} to GitHub', (notebookTitle: string) => {
  start.notebookCard(notebookTitle).exportToGithub()
})

When('I input repository name {string}', (repoName: string) => {
  // Mock GitHub repository data
  start.testability().mockGithubRepos([
    {
      name: repoName,
      full_name: `test-user/${repoName}`,
      permissions: { push: true },
    },
  ])

  cy.findByLabelText('Select Repository').click().findByText(repoName).click()
  cy.findByRole('button', { name: 'Export' }).click()
})

Then('I should see a success message {string}', (message: string) => {
  cy.findByText(message).should('be.visible')
})
