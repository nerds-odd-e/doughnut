export const recall = () => {
  const getRecallListItemInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => cy.get('.sidebar-control').within(() => fn(cy.get('li[title="Recall"]')))

  return {
    goToRecallPage() {
      cy.routerToRoot()
      getRecallListItemInSidebar(($el) => {
        $el.click()
      })
      return this
    },
  }
}
