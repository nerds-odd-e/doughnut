/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

/*eslint-disable */
Given("I create a note", () => {
 })

Given("there is a book {string} authored by {string} with WikiData ID {string}",
 (bookTitle: string, authorName: string, qid: string) => {
 })

Given("there is a book {string} unauthored with WikiData ID {string}",
(bookTitle: string, qid: string) => {
})

When("I create the note {string} associated with WikiData ID {string}",
(bookTitle: string, qid: string) => {
})

Then("I expect a note {string} with a note {string} as the author of the book {string}",
(bookTitle: string, authorName: string, linkedBookTitle: string) => {
})

Then("I expect a note {string} with no linked note to an author.",
(bookTitle: string) => {
})

Given("I've logged in", () => {
})
/*eslint-enable */
