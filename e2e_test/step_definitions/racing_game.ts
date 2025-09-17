/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I am a player and in the game screen, round 0', function () {
  cy.visit('/d/car-game/abc')
  cy.findByText('0', { selector: '.player-position' }).should('exist')
  // TODO: implement step
})
