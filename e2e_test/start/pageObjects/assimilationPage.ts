export const assimilation = () => {
  const getAssimilateListItemInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) =>
    cy
      .get('.sidebar-control')
      .within(() => fn(cy.get('li[title="Assimilate"]')))

  return {
    expectCount(numberOfNotes: number) {
      getAssimilateListItemInSidebar(($el) => {
        $el.findByText(`${numberOfNotes}`, { selector: '.due-count' })
      })
      return this
    },
    goToAssimilationPage() {
      cy.routerToRoot()
      getAssimilateListItemInSidebar(($el) => {
        $el.click()
      })
      return {
        expectToAssimilateAndTotal(toAssimilateAndTotal: string) {
          const [assimlatedTodayCount, toAssimilateCountForToday] =
            toAssimilateAndTotal.split('/')
          cy.get('.progress-bar').should(
            'contain',
            `Assimilating: ${assimlatedTodayCount}/${toAssimilateCountForToday}`
          )
        },
      }
    },
  }
}
