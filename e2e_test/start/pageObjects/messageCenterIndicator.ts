import { assumeMessageCenterPage } from './messageCenterPage'
import { waitUntilAppIsNotBusy, withinDonutMenuItem } from '../pageBase'

export function waitForMenuDataUnreadCount() {
  cy.intercept('GET', '**/api/user/menu-data**').as('menuDataForUnreadCount')
}

export function messageCenterIndicator() {
  const getMessagesInDonutMenu = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => withinDonutMenuItem('Messages', fn)

  return {
    expectCount(unreadMessageCount: number) {
      getMessagesInDonutMenu(($el) => {
        $el
          .findByText(`${unreadMessageCount}`, { selector: '.unread-count' })
          .should(($badge) => {
            expect(
              $badge.text().trim(),
              `Expected unread message count ${unreadMessageCount}, but found ${$badge.text().trim()}`
            ).to.equal(`${unreadMessageCount}`)
          })
      })
      return this
    },
    expectNoCount() {
      getMessagesInDonutMenu(($el) => {
        $el.get('.unread-count').should('not.exist')
      })
      return this
    },
    go() {
      getMessagesInDonutMenu(($el) => {
        $el.click()
      })
      waitUntilAppIsNotBusy()
      return assumeMessageCenterPage()
    },
  }
}
