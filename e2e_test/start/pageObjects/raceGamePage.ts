import { cy } from 'local-cypress'

export const assumeRaceGamePage = () => ({
  rollDice() {
    cy.findByRole('button', { name: 'Roll Dice' }).click()
    cy.pageIsNotLoading()
    return this
  },

  resetGame() {
    cy.findByRole('button', { name: 'Reset Game' }).click()
    cy.pageIsNotLoading()
    return this
  },

  expectCarPosition(position: number) {
    cy.get('[data-testid="car-position"]').should(
      'have.text',
      position.toString()
    )
    return this
  },

  expectDiceOutcome(outcome: number) {
    cy.get('[data-testid="dice-outcome"]').should(
      'have.text',
      outcome.toString()
    )
    return this
  },

  expectGameWon() {
    cy.get('[data-testid="game-status"]').should('have.text', 'You won!')
    return this
  },

  expectRoundCount(count: number) {
    cy.get('[data-testid="round-count"]').should('have.text', count.toString())
    return this
  },
})

export const raceGame = () => ({
  routerToRaceGamePage() {
    cy.visit('/d/race')
    cy.pageIsNotLoading()
    return assumeRaceGamePage()
  },
})

export default raceGame
