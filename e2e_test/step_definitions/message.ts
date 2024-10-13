import { Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then(
  'I reply {string} to the conversation {string}',
  (message: string, conversation: string) => {
    start.navigateToMessageCenter().replyInConversation(conversation, message)
  }
)

Then(
  "I should see the new message {string} on the current user's side of the conversation",
  (message: string) => {
    start.assumeMessageCenterPage().expectMessageDisplayAtUserSide(message)
  }
)

Then(
  "I should see the new message {string} on the other user's side of the conversation",
  (message: string) => {
    start.assumeMessageCenterPage().expectMessageDisplayAtOtherSide(message)
  }
)
