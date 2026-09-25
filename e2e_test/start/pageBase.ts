/// <reference types="cypress" />
// @ts-check

import router from './router'

/** Waits until no [data-app-busy] loading UI remains. */
export const waitUntilAppIsNotBusy = () => {
  cy.get('[data-app-busy]', { timeout: 30000 }).should('not.exist')
}

/** Runs `fn` on the main menu item titled `title`, opening the app first when needed. */
export const withinMainMenuItem = (
  title: string,
  fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
) => {
  router().openApp()
  cy.get('.main-menu').within(() => fn(cy.get(`li[title="${title}"]`)))
}
