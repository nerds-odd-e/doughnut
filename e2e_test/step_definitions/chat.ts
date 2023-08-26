/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import pageObjects from "../page_objects"

import { When, Then } from "@badeball/cypress-cucumber-preprocessor"

When("I send the message {string} to AI", (question: string) => {
  pageObjects.chatAboutNotePage().sendMessage(question)
})

Then("I should receive the response {string}", (answer: string) => {
  pageObjects.chatAboutNotePage().expectResponse(answer)
})
