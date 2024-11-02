/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import start from '../start'

import { DataTable, Then, When } from '@badeball/cypress-cucumber-preprocessor'

When('I send the message {string} to AI', (question: string) => {
  start.assumeChatAboutNotePage().replyToConversation(question)
})

When('I send the message {string} to AI 1', (question: string) => {
  start.assumeChatAboutNotePage().replyToConversationNew(question)
})

Then('I should receive the following chat messages:', (data: DataTable) => {
  start.assumeChatAboutNotePage().expectMessages(data.hashes())
})
