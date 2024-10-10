/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import start from '../start'

import { DataTable, Then, When } from '@badeball/cypress-cucumber-preprocessor'

When('I send the message {string} to AI', (question: string) => {
  start.assumeChatAboutNotePage().sendMessage(question)
})

When(
  'I ask the question {string} to AI in the conversation',
  (question: string) => {
    start.assumeChatAboutNotePage().askAI(question)
  }
)

Then('I should receive the following chat messages:', (data: DataTable) => {
  start.assumeChatAboutNotePage().expectMessages(data.hashes())
})

Then("I ask for AI's response", () => {
  start.assumeChatAboutNotePage().askAI();
})
