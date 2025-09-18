/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'

Given('I am a player and in the game screen, round 0', function () {
  cy.visit('/d/car-game/abc')
  cy.findByText('0', { selector: '.player-position' }).should('exist')
  // TODO: implement step
})

When('a new player joins', function () {
  cy.visit('/d/car-game/abc')
})

Then('the player count is {string}', (numberOfPlayers: string) => {
  cy.get('#number-of-players').should('contain.text', numberOfPlayers)
})

When(
  'I choose to play the normal mode and roll the dice',
  function (this: { initialPosition: number }) {
    // Get the initial position value before rolling dice
    cy.get('.player-position')
      .invoke('text')
      .then((initialPosition) => {
        cy.log('Initial position:', initialPosition)
        this.initialPosition = parseInt(initialPosition.trim())
      })

    cy.get('#roll-dice-button').click()

    // Wait for the dice roll animation to complete
    cy.get('#roll-dice-button').should('not.be.disabled')

    // Get the new position value after rolling dice
    cy.get('.player-position')
      .invoke('text')
      .then((newPosition) => {
        cy.log('New position:', newPosition)
        const newPositionNum = parseInt(newPosition.trim())

        // Assert that the position has changed (car moved)
        expect(newPositionNum).to.be.greaterThan(this.initialPosition)
      })
  }
)

Then('the total damage becomes {int}', function (number) {
  // Get the text content and compare it to the expected number
  cy.get('.dammage-position')
    .invoke('text')
    .then((damageText) => {
      const damageValue = parseInt(damageText.trim())
      expect(damageValue).to.equal(number)
    })
})

Then('the dice number has value in range 1-6', () => {
  cy.get('#dice-result-display')
    .invoke('text')
    .should((text) => {
      expect(Number(text)).to.be.greaterThan(0).and.to.be.lessThan(7)
    })
})

Then('the car moves 1 or 2 steps', () => {
  cy.get('.player-position')
    .invoke('text')
    .then((positionText) => {
      const positionValue = parseInt(positionText.trim())
      expect(positionValue).to.be.greaterThan(0).and.to.be.lessThan(3)
    })
})
