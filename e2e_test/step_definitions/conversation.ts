import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import type { DataTable } from '@cucumber/cucumber'

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
  'I read the conversation with {string} for the subject {string} in the message center',
  (partner: string, subject: string) => {
    start
      .navigateToMessageCenter()
      .expectConversation(subject, partner)
      .conversation(subject)
  }
)

Then(
  '{string} can see the conversation with {string} for the subject {string} in the message center:',
  (user: string, partner: string, subject: string, data: DataTable) => {
    const loginResult = start.reloginAndEnsureHomePage(user)
    cy.wrap(loginResult).then(() => {
      start
        .navigateToMessageCenter()
        .expectConversation(subject, partner)
        .conversation(subject)
        .expectMessage(data.hashes()[0]!.message!)
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

Then(
  'there should be no unread message for the user {string}',
  (user: string) => {
    const loginResult = start.reloginAndEnsureHomePage(user)
    cy.wrap(loginResult).then(() => {
      start.messageCenterIndicator().expectNoCount()
    })
  }
)

Then('I should have no unread messages', () => {
  start.messageCenterIndicator().expectNoCount()
})

Then(
  '{string} should have {int} unread messages',
  (user: string, unreadMessageCount: number) => {
    const loginResult = start.reloginAndEnsureHomePage(user)
    cy.wrap(loginResult).then(() => {
      start.messageCenterIndicator().expectCount(unreadMessageCount)
    })
  }
)

When(
  '{string} start a conversation about the note {string} with a message {string}',
  (externalIdentifier: string, note: string, conversation: string) => {
    const loginResult = start.reloginAndEnsureHomePage(externalIdentifier)
    cy.wrap(loginResult).then(() => {
      start.jumpToNotePage(note).sendMessageToNoteOwner(conversation)
    })
  }
)

When(
  'I start a conversation about the note {string} with a message {string} to AI',
  (note: string, conversation: string) => {
    start.jumpToNotePage(note).sendMessageToAI(conversation)
  }
)

When('I send the message {string} to AI', (question: string) => {
  start
    .assumeConversationAboutNotePage()
    .replyToConversationAndInviteAiToReply(question)
})

Then('I should receive the following chat messages:', (data: DataTable) => {
  start.assumeConversationAboutNotePage().expectMessages(data.hashes())
})

When('I export the conversation', () => {
  start.assumeConversationAboutNotePage().exportConversation()
})

Then(
  'the export should contain the conversation title {string}',
  (title: string) => {
    start.assumeConversationAboutNotePage().expectExportContainsTitle(title)
  }
)

Then(
  'the export should contain the user message {string}',
  (message: string) => {
    start
      .assumeConversationAboutNotePage()
      .expectExportContainsUserMessage(message)
  }
)

Then(
  'the export should contain the assistant reply {string}',
  (reply: string) => {
    start
      .assumeConversationAboutNotePage()
      .expectExportContainsAssistantReply(reply)
  }
)

Then('I should be able to copy the export', () => {
  start.assumeConversationAboutNotePage().copyExport()
})
