import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'

Given('I am at the beginning of the race game', () => {
  cy.visit('/d/race')
  cy.get('#car-position')
    .should('exist')
    .then(($el) => {
      const position = parseInt($el.text())
      expect(position).to.be.eq(0)
    })
})

When('I choose to go normal for this round', () => {
  cy.get('#car-position')
    .should('exist')
    .then(($el) => {
      const initialPosition = parseInt($el.text())
      cy.wrap(initialPosition).as('initialPosition')
    })
  cy.contains('button', 'GO NORMAL').click()
})

Then(
  'my car should move no further than 2 steps at round {int}',
  (round: number) => {
    cy.findByText(round, { selector: '#round-count' }).should('exist')
    cy.get<number>('@initialPosition').then((initialPosition) => {
      cy.get('#car-position')
        .should('exist')
        .then(($el) => {
          const currentPosition = parseInt($el.text())
          const steps = currentPosition - initialPosition
          expect(steps).to.be.at.most(2)
          expect(steps).to.be.at.least(1)
        })
    })
  }
)

When('I reset the game', () => {
  cy.contains('button', 'RESET').click()
})

Then(
  'my car should at the beginning of the race game and the round count is 0',
  () => {
    cy.findByText('0', { selector: '#round-count' }).should('exist')
    cy.get('#car-position')
      .should('exist')
      .then(($el) => {
        const position = parseInt($el.text())
        expect(position).to.be.eq(0)
      })
  }
)
