import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { DataTable } from '@cucumber/cucumber'

Then(
  'I reply {string} to the conversation {string}',
  (message: string, conversation: string) => {
    start.navigateToMessageCenter().conversation(conversation).reply(message)
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
  '{string} read the conversation with {string} for the topic {string} in the message center',
  (user: string, partner: string, topic: string) => {
    start
      .reloginAndEnsureHomePage(user)
      .navigateToMessageCenter()
      .expectConversation(topic, partner)
      .conversation(topic)
  }
)

Then(
  '{string} can see the conversation with {string} for the topic {string} in the message center:',
  (user: string, partner: string, topic: string, data: DataTable) => {
    start
      .reloginAndEnsureHomePage(user)
      .navigateToMessageCenter()
      .expectConversation(topic, partner)
      .conversation(topic)
      .expectMessage(data.hashes()[0]!.message!)
  }
)

Then(
  'all circle members {string} can view the conversation with {string} for the note {string} in the message center',
  (members: string, circleName: string, note: string) => {
    members.split(', ').forEach((member) => {
      start
        .reloginAndEnsureHomePage(member)
        .navigateToMessageCenter()
        .expectConversation(note, circleName)
    })
  }
)

Then(
  'I can see the message {string} in the conversation {string}',
  (message: string, conversation: string) => {
    start
      .assumeMessageCenterPage()
      .conversation(conversation)
      .expectMessage(message)
  }
)

Then('The current page is reloaded', () => {
  start.assumeMessageCenterPage().reloadCurrentPage()
})

Then(
  'there should be no unread message for the user {string}',
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
  '{string} start a conversation about the note {string} with a message {string}',
  (externalIdentifier: string, note: string, conversation: string) => {
    start
      .loginAs(externalIdentifier)
      .jumpToNotePage(note)
      .sendMessageToNoteOwner(conversation)
  }
)
