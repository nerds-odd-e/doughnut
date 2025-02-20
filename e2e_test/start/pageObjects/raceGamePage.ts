export const assumeRaceGamePage = () => ({
  rollDice() {
    cy.findByRole('button', { name: 'GO NORMAL' }).click()
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
    cy.get('#car-position')
      .should('exist')
      .then(($el) => {
        const position = parseInt($el.text())
        expect(position).to.be.at.most(steps)
        expect(position).to.be.at.least(1)
      })
    return this
  },
})

export const routerToRaceGamePage = () => {
  cy.visit('/d/race')
  cy.pageIsNotLoading()
  return assumeRaceGamePage()
}
