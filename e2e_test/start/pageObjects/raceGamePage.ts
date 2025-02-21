export const assumeRaceGamePage = () => ({
  rollDice(isSuper: boolean = false) {
    cy.get('#car-position')
      .should('exist')
      .then(($el) => {
        const initialPosition = parseInt($el.text())
        cy.wrap(initialPosition).as('initialPosition')
      })
    cy.findByRole('button', { name: isSuper ? 'GO SUPER' : 'GO NORMAL' }).click()
    cy.pageIsNotLoading()
    return this
  },

  resetGame() {
    cy.findByRole('button', { name: 'GO NORMAL' }).click()
    cy.pageIsNotLoading()
    return this
  },

  expectCarPosition(steps: number, round: number) {
    cy.findByText(round, { selector: '#round-count' }).should('exist')
    cy.get<number>('@initialPosition').then((initialPosition) => {
      cy.get('#car-position')
        .should('exist')
        .then(($el) => {
          const currentPosition = parseInt($el.text())
          const moveDistance = currentPosition - initialPosition
          expect(moveDistance).to.be.at.most(steps)
          expect(moveDistance).to.be.at.least(1)
        })
    })
    return this
  },

  joinAs(name: string) {
    cy.findByRole('textbox', { name: /name/i }).type(name)
    cy.findByRole('button', { name: 'JOIN' }).click()
    cy.pageIsNotLoading()
    return this
  },

  expectCarBelongsTo(name: string) {
    cy.findByText(`${name}`).should('exist')
    return this
  },
})

export const routerToRaceGamePage = () => {
  cy.visit('/d/race')
  cy.pageIsNotLoading()
  return assumeRaceGamePage()
}
