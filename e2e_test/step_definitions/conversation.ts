import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import mock_services from '../start/mock_services/index'
import start from '../start'
import { waitForMenuDataUnreadCount } from '../start/pageObjects/messageCenterIndicator'
import type { DataTable } from '@cucumber/cucumber'

function expectConversationInMessageCenter(
  subject: string,
  partner: string,
  message: string,
  options: { reloginAs?: string } = {}
) {
  const open = () =>
    start
      .navigateToMessageCenter({ expectedSubject: subject })
      .openConversation(subject, partner)
      .expectMessage(message)

  if (options.reloginAs) {
    start.reloginAs(options.reloginAs).then(() => {
      open()
    })
  } else {
    open()
  }
}

When(
  'I reply to the conversation {string}:',
  (conversation: string, data: DataTable) => {
    const messages = data.raw().map((row) => row[0]!.trim())
    return start
      .testability()
      .replyToConversationAboutNote(conversation, messages)
  }
)

When(
  'I read the conversation with {string} about {string}',
  (partner: string, subject: string) => {
    start.navigateToMessageCenter().openConversation(subject, partner)
  }
)

Then(
  '{string} can see the conversation with {string} about {string} in the message center:',
  (user: string, partner: string, subject: string, data: DataTable) => {
    expectConversationInMessageCenter(
      subject,
      partner,
      data.hashes()[0].message!,
      { reloginAs: user }
    )
  }
)

Then(
  'I can see the conversation with {string} about {string} in the message center:',
  (partner: string, subject: string, data: DataTable) => {
    expectConversationInMessageCenter(
      subject,
      partner,
      data.hashes()[0].message!
    )
  }
)

Then('I should have no unread messages', () => {
  start.messageCenterIndicator().expectNoCount()
})

Then(
  '{string} should have an unread message count of {int}',
  (user: string, unreadMessageCount: number) => {
    waitForMenuDataUnreadCount()
    start.reloginAs(user)
    cy.wait('@menuDataForUnreadCount')
    start.messageCenterIndicator().expectCount(unreadMessageCount)
  }
)

When(
  '{string} starts a conversation about the note {string} with the message {string}',
  (externalIdentifier: string, note: string, conversation: string) => {
    start.reloginAs(externalIdentifier)
    return start.testability().startConversationAboutNote(note, conversation)
  }
)

When('I start a conversation about this note', () => {
  start.assumeNotePage().startAConversationAboutNote()
})

When('I close the conversation', () => {
  start.assumeNotePage().closeConversation()
})

When(
  'I start a conversation about the note {string} inviting AI with the message {string}',
  (note: string, conversation: string) => {
    start.jumpToNotePage(note).sendMessageToAI(conversation)
  }
)

When('I send the message {string} to AI', (question: string) => {
  start
    .assumeConversationAboutNotePage()
    .replyToConversationAndInviteAiToReply(question)
})

Then('I should see the following messages:', (data: DataTable) => {
  start.assumeConversationAboutNotePage().expectMessages(data.hashes())
})

Then('OpenAI responses were called with Donut focus context', () => {
  mock_services.openAi().expectLastResponsesPostBodyContains('<focus_context>')
})

When('I export the conversation', () => {
  start.assumeConversationAboutNotePage().exportConversation()
})

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
