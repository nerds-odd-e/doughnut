/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then('I am on a window {int} * {int}', (width: number, height: number) => {
  cy.viewport(width, height)
})

function expandSideBar() {
  start.noteSidebar()
}

When('I expand the side bar', expandSideBar)

When('I open the note {string} from the sidebar', (noteTopology: string) => {
  start.noteSidebar().navigateToNote(noteTopology)
})

Then('I should see the note tree in the sidebar', (data: DataTable) => {
  start.noteSidebar().expectOrderedNotes(data.hashes())
})

Then('I move the note {string} up among its siblings', (noteToMove: string) => {
  start.jumpToNotePage(noteToMove).moveUpAmongSiblings()
})

Then(
  'I move the note {string} down among its siblings',
  (noteToMove: string) => {
    start.navigateToNotebooksPage()
    start.jumpToNotePage(noteToMove).moveDownAmongSiblings()
  }
)

Then(
  'I should see the note {string} before the note {string} in the sidebar',
  (noteMoved: string, noteStayed: string) => {
    start.noteSidebar().siblingOrder(noteMoved, noteStayed)
  }
)
