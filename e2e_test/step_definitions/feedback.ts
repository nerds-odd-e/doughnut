import { Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then(
  '{string} can see the conversation {string} with {string} in the message center',
  (user: string, feedback: string, partner: string) => {
    start
      .reloginAndEnsureHomePage(user)
      .navigateToMessageCenter()
      .expectMessage(feedback, partner)
  }
)
