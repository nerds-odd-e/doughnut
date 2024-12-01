export const assimilation = () => {
  return {
    expectCount(numberOfNotes: number) {
      cy.get('.sidebar-control').within(() => {
        cy.get('li[title="Assimilate"]').within(() => {
          cy.findByText(`${numberOfNotes}`, { selector: '.due-count' })
        })
      })
    },
    expectToAssimilateAndTotal(_toAssimilateAndTotal: string) {
      //
    },
  }
}
