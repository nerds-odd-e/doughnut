import { assumeMessageCenterPage } from './messageCenterPage'
import { waitUntilAppIsNotBusy } from '../pageBase'

export function waitForMenuDataUnreadCount() {
  cy.intercept('GET', '**/api/user/menu-data**').as('menuDataForUnreadCount')
}

export function messageCenterIndicator() {
  const getMessageInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => cy.get('.main-menu').within(() => fn(cy.get('li[title="Messages"]')))

  return {
    expectCount(unreadMessageCount: number) {
      getMessageInSidebar(($el) => {
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
      getMessageInSidebar(($el) => {
        $el.get('.unread-count').should('not.exist')
      })
      return this
    },
    go() {
      getMessageInSidebar(($el) => {
        $el.click()
      })
      waitUntilAppIsNotBusy()
      return assumeMessageCenterPage()
    },
  }
}
