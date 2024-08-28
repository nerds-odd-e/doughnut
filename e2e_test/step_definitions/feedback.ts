import { Given, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I visit the feedback page', (userType: string) => {
  console.log(userType)
  start.systemSidebar().userOptions().myFeedbackOverview()
})

// When('I have received feedback on a question', () => {})

// When('I open that conversation', () => {})

// Then('I should be able to respond', () => {})

Then('I should see the feedback message {string}', (feedback: string) => {
  cy.findByText(feedback).should('exist')
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
