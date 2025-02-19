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
  cy.contains('button', 'GO NORMAL').click()
})

Then(
  'my car should move no further than 2 steps at round {int}',
  (round: number) => {
    cy.findByText(round, { selector: '#round-count' }).should('exist')
    cy.get('#car-position')
      .should('exist')
      .then(($el) => {
        const position = parseInt($el.text())
        expect(position).to.be.at.most(2)
        expect(position).to.be.at.least(1)
      })
  }
)
