/// <reference types="cypress" />
// @ts-check

import router from './router'

/** Waits until no [data-app-busy] loading UI remains. */
export const waitUntilAppIsNotBusy = () => {
  cy.get('[data-app-busy]', { timeout: 30000 }).should('not.exist')
}

const donutMenuPanel = '[role="region"][aria-label="Donut menu panel"]'

/** The Donut menu launcher button at the start of the page bar. */
const donutMenuLauncher = () => cy.findByRole('button', { name: 'Donut menu' })

/** The shared page bar, which starts with the Donut menu launcher. */
export const pageBar = () => donutMenuLauncher().closest('nav')

const pressDonutMenuLauncherWhen = (panelOpen: boolean) =>
  cy.get('body').then(($body) => {
    if ($body.find(donutMenuPanel).length > 0 === panelOpen) {
      donutMenuLauncher().click()
    }
  })

/** Opens the Donut menu panel from the page bar unless it is already open. */
export const openDonutMenu = () => {
  pressDonutMenuLauncherWhen(false)
  return cy.get(donutMenuPanel)
}

/** Runs `fn` on the Donut menu item titled `title`, opening the app and the panel first, and closes the panel afterwards when `fn` left it open. */
export const withinDonutMenuItem = (
  title: string,
  fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
) => {
  router().openApp()
  openDonutMenu().within(() => fn(cy.get(`li[title="${title}"]`)))
  pressDonutMenuLauncherWhen(true)
}
