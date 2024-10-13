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

Then(
  '{string} can see the conversation with {string} for the question {string} in the message center',
  (user: string, partner: string, question: string) => {
    start
      .reloginAndEnsureHomePage(user)
      .navigateToMessageCenter()
      .expectMessage(question, partner)
  }
)

Then(
  'I can see the message {string} in the conversation {string}',
  (feedback: string, conversation: string) => {
    start.assumeMessageCenterPage().clickToSeeExpectMessage(conversation, feedback)
  }
)
