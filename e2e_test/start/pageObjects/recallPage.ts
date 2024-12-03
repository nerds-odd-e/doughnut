const recallPage = () => {
  return {
    expectToRecallCounts(numberOfRecalls: string) {
      const [recalledTodayCount, toRecallCountForToday, totalCount] =
        numberOfRecalls.split('/')

      cy.get('.progress-bar').should(
        'contain',
        `Recalling: ${recalledTodayCount}/${toRecallCountForToday}`
      )
      // Click progress bar to show tooltip
      cy.get('.progress-bar').first().click()

      // Check tooltip content
      cy.get('.tooltip-content').within(() => {
        cy.contains(
          `Daily Progress: ${recalledTodayCount} / ${toRecallCountForToday}`
        )
        cy.contains(`Total assimilated: ${recalledTodayCount} / ${totalCount}`)
      })

      // Close tooltip
      cy.get('.tooltip-popup').click()
    },
    repeatMore() {
      cy.findByRole('button', { name: 'Load more from next 3 days' }).click()
    },
  }
}
export const recall = () => {
  const getRecallListItemInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => cy.get('.sidebar-control').within(() => fn(cy.get('li[title="Recall"]')))

  return {
    expectCount(numberOfNotes: number) {
      getRecallListItemInSidebar(($el) => {
        $el.findByText(`${numberOfNotes}`, { selector: '.recall-count' })
      })
      return this
    },
    goToRecallPage() {
      cy.routerToRoot()
      getRecallListItemInSidebar(($el) => {
        $el.click()
      })
      return recallPage()
    },
  }
}
