import { assumeMessageCenterPage } from './messageCenterPage'

export function messageCenterIndicator() {
  const getMessageInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => cy.get('.main-menu').within(() => fn(cy.get('li[title="Messages"]')))

  return {
    expectCount(numberOfNotes: number) {
      getMessageInSidebar(($el) => {
        $el.findByText(`${numberOfNotes}`, { selector: '.unread-count' })
      })
      return this
    },
    expectNoCount() {
      getMessageInSidebar(($el) => {
        $el.get('.unread-count').should('not.exist')
      })
    },
    go() {
      getMessageInSidebar(($el) => {
        $el.click()
      })
      return assumeMessageCenterPage()
    },
  }
}
