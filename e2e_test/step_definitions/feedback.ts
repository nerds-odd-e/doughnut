import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I visit the feedback page', (userType: string) => {
  console.log(userType)
  start.systemSidebar().userOptions().myFeedbackOverview()
})

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

When('I open that conversation', () => {
  cy.findByText('There is no feedback currently.').should('not.exist')
  cy.findByRole('link', {
    name: 'View chat',
  }).click()
  cy.findByText("I don't understand this question").should('be.visible')
})

Then(
  '{string} can see the feedback {string}',
  (user: string, feedback: string) => {
    start.reloginAndEnsureHomePage(user).navigateToFeedbackOverviewPage()
    cy.reload()
    cy.findByText(feedback).should('be.visible')
  }
)

Then(
  '{string} {string} see the conversation about question {string}',
  (user: string, canOrCannotSee: string, feedback: string) => {
    start.reloginAndEnsureHomePage(user).navigateToFeedbackOverviewPage()
    if (canOrCannotSee === 'can') {
      cy.findByText(feedback).should('be.visible')
    } else {
      cy.findByText(feedback).should('not.exist')
    }
  }
)

Then(
  '{string} can see name {string} in the conversation',
  (loggedInUser: string, partnerUser: string) => {
    start
      .reloginAndEnsureHomePage(loggedInUser)
      .navigateToFeedbackOverviewPage()
    cy.findByText(partnerUser).should('be.visible')
  }
)

Then('I see the message {string}', (message: string) => {
  cy.findByText(message).should('exist')
})
