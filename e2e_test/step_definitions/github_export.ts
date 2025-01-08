import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

// No need to implement "Given I am logged in as an existing user"
// as it's already implemented in user.ts

Given('I have authorized Doughnut to access my GitHub account', () => {
  // Mock GitHub OAuth authorization
  start.testability().mockGithubAuth({
    authorized: true,
    token: 'mock-github-token',
  })
})

Given('I have a GitHub repository {string}', (repoName: string) => {
  // Mock GitHub repository data
  start.testability().mockGithubRepos([
    {
      name: repoName,
      full_name: `test-user/${repoName}`,
      permissions: { push: true },
    },
  ])
})

When(
  'I choose to export notebook {string} to GitHub',
  (notebookTitle: string) => {
    start.routerToNotebooksPage().notebookCard(notebookTitle).exportToGithub()
  }
)

When('I select repository {string}', (repoName: string) => {
  cy.findByLabelText('Select Repository').click().findByText(repoName).click()

  cy.findByRole('button', { name: 'Export' }).click()
})

Then('I should see a success message {string}', (message: string) => {
  cy.findByText(message).should('be.visible')
})
