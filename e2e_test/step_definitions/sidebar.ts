/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { DataTable, Then } from "@badeball/cypress-cucumber-preprocessor"
import start from "../start"

Then("I am on a window {int} * {int}", (width: number, height: number) => {
  cy.viewport(width, height)
})

Then("I expand the side bar", () => {
  cy.findByRole("button", { name: "toggle sidebar" }).click()
})

Then("I should see the note tree in the sidebar", (data: DataTable) => {
  start.sidebar().expectItems(data.hashes())
})

Then("I move the note {string} up among its siblings", (noteToMove: string) => {
  start.jumpToNotePage(noteToMove).moveUpAmongSiblings()
})

Then(
  "I should see the note {string} before the note {string} in the sidebar",
  (noteMoved: string, noteStayed: string) => {
    start.sidebar().siblingOrder(noteMoved, noteStayed)
  },
)
