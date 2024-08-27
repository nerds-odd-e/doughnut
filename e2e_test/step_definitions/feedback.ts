import { Given } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I visit the feedback page', (userType: string) => {
  console.log(userType)
  start.systemSidebar().userOptions().myFeedbackOverview()
})

// When('I have received feedback on a question', () => {})

// When('I open that conversation', () => {})

// Then('I should be able to respond', () => {})
