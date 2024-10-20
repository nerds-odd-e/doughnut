export function messageCenterIndicator() {
  const counter = cy.get('#top-navbar-message-icon').get('.unread-count')

  return {
    expectNoCount() {
      counter.should('not.exist')
    },
    expectCount(unreadMessageCount: number) {
      counter.should('contain', unreadMessageCount)
    },
  }
}
