import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
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

Then(
  '{string} can see the conversation with {string} for the topic {string} in the message center',
  (user: string, partner: string, topic: string) =>
    findConversation(user, topic, partner)
)

Then(
  'all circle members {string} can view the message {string} in conversation with {string} for the note {string} in the message center',
  (members: string, message: string, circleName: string, note: string) => {
    members.split(', ').forEach((member) => {
      findConversation(member, note, circleName)
      start
        .assumeMessageCenterPage()
        .clickToSeeExpectMessage(circleName, message)
    })
  }
)

function findConversation(user: string, topic: string, partner: string) {
  start
    .reloginAndEnsureHomePage(user)
    .navigateToMessageCenter()
    .expectConversation(topic, partner)
}

Then(
  'I can see the message {string} in the conversation with {string}',
  (message: string, partner: string) => {
    start.assumeMessageCenterPage().clickToSeeExpectMessage(partner, message)
  }
)

Then(
  'I can see the message {string} in the conversation {string}',
  (feedback: string, conversation: string) => {
    start
      .assumeMessageCenterPage()
      .clickToSeeExpectMessage(conversation, feedback)
  }
)

Then('The current page is reloaded', () => {
  start.assumeMessageCenterPage().reloadCurrentPage()
})

Then(
  '{string} can see the notification icon with no unread messages',
  (user: string) => {
    start.reloginAndEnsureHomePage(user).checkForMessageCenterIcon()
  }
)

Then(
  '{string} can see the notification icon with {int} unread messages',
  (user: string, unreadMessageCount: number) => {
    start
      .reloginAndEnsureHomePage(user)
      .checkForUnreadMessageCount(unreadMessageCount)
  }
)

When(
  'I start a conversation about the note {string} with a message {string}',
  (note: string, conversation: string) => {
    start.jumpToNotePage(note).sendMessageToNoteOwner(conversation)
  }
)
