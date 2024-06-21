/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import start from "../start"

import { When, Then, DataTable } from "@badeball/cypress-cucumber-preprocessor"

When("I send the message {string} to AI", (question: string) => {
  start.assumeChatAboutNotePage().sendMessage(question)
})

Then("I should receive the response {string}", (answer: string) => {
  start.assumeChatAboutNotePage().expectResponse(answer)
})

Then("I should receive the following chat messages:", (data: DataTable) => {
  start.assumeChatAboutNotePage().expectMessages(data.hashes())
})
