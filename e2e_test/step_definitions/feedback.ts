import { Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then(
  '{string} can see the feedback {string}',
  (user: string, feedback: string) => {
    start
      .reloginAndEnsureHomePage(user)
      .navigateToMessageCenter()
      .expectMessage(feedback)
  }
)

Then(
  '{string} can see name {string} in the message center',
  (loggedInUser: string, partnerUser: string) => {
    start.reloginAndEnsureHomePage(loggedInUser).navigateToMessageCenter()
    cy.findByText(partnerUser).should('be.visible')
  }
)

Then('I see the message {string}', (message: string) => {
  cy.findByText(message).should('exist')
})
