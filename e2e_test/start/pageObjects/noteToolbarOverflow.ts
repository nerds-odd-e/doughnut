const overflowMenuTitle = 'more options'

export const noteToolbar = () =>
  cy.get('[data-note-toolbar]', { timeout: 15000 })

export const visibleToolbarActionButton = (title: string) =>
  cy.get(`button[title="${title}"]:visible`, { timeout: 15000 }).first()

export const openToolbarOverflowMenuIfNeeded = (title: string) => {
  noteToolbar()
    .should('exist')
    .then(() => {
      if (Cypress.$(`button[title="${title}"]:visible`).length > 0) {
        return
      }

      noteToolbar()
        .find(`summary[title="${overflowMenuTitle}"]`)
        .should('be.visible')
        .click()
    })
}

export const clickToolbarOverflowAction = (title: string) => {
  openToolbarOverflowMenuIfNeeded(title)
  visibleToolbarActionButton(title).scrollIntoView().click()
}
