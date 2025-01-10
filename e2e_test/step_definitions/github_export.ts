import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('I export notebook {string} to GitHub', (notebookTitle: string) => {
  start.routerToNotebooksPage()
  start.notebookCard(notebookTitle).exportToGitHub()
})

When('I input repository name {string}', (repoName: string) => {
  start.notebookCard('').inputGitHubRepository(repoName)
})

Then('I should see a success message {string}', (message: string) => {
  start.expectToast(message)
})
