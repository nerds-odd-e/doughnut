import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I visit the feedback page', (userType: string) => {
  console.log(userType)
  start.systemSidebar().userOptions().myFeedbackOverview()
})

// When('I have received feedback on a question', () => {})

// When('I open that conversation', () => {})

// Then('I should be able to respond', () => {})

Given(
  "Pete has given the feedback I don't understand this question on {string}",
  (question: string) => {
    cy.findByText(question).should('be.visible')
  }
)

When('I open feedback on {string}', (question: string) => {
  cy.findByText(question)
    .parent()
    .findByRole('link', {
      name: 'View',
    })
    .click()
})

Then(
  '{string} can see the feedback {string} on the question {string}',
  (user: string, feedback: string, question: string) => {
    cy.loginAs(user)
    start.systemSidebar().userOptions().myFeedbackOverview()
    cy.findByText(feedback).should('be.visible')
    cy.findByText(question).should('be.visible')
  }
)

Then('I see the message {string}', (message: string) => {
  cy.findByText(message).should('exist')
})
