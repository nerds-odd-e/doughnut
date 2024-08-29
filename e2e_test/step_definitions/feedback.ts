import { Given, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I visit the feedback page', (userType: string) => {
  console.log(userType)
  start.systemSidebar().userOptions().myFeedbackOverview()
})

// When('I have received feedback on a question', () => {})

// When('I open that conversation', () => {})

// Then('I should be able to respond', () => {})

Given(
  'Pete has given the feedback {string} on a question on notebook {string}',
  (feedback: string, notebook: string) => {
    //Start assessment
    start.navigateToBazaar().selfAssessmentOnNotebook(notebook)
    //Select wrong answer
    cy.findByRole('button', { name: 'No' }).click()
    //Submit feedback
    cy.findByText('Send feedback').click()
    cy.findByPlaceholderText('Give feedback about the question').type(feedback)
    cy.findByRole('button', { name: 'Submit' }).click()
  }
)

Then(
  '{string} can see the feedback {string}',
  (user: string, feedback: string) => {
    cy.loginAs(user)
    start.systemSidebar().userOptions().myFeedbackOverview()
    cy.findByText(feedback).should('be.visible')
  }
)

Then(
  '{string} {string} see the conversation about question {string}',
  (user: string, canOrCannotSee: string, feedback: string) => {
    cy.loginAs(user)
    start.systemSidebar().userOptions().myFeedbackOverview()
    if (canOrCannotSee === 'can') {
      cy.findByText(feedback).should('be.visible')
    } else {
      cy.findByText(feedback).should('not.exist')
    }
  }
)

Then(
  "{string} can see {string}'s name in the conversation",
  (loggedInUser: string, partnerUser: string) => {
    cy.loginAs(loggedInUser)
    start.systemSidebar().userOptions().myFeedbackOverview()
    cy.findByText(partnerUser).should('be.visible')
  }
)

Then('I see the message {string}', (message: string) => {
  cy.findByText(message).should('exist')
})
