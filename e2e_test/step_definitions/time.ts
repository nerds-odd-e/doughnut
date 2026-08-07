/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('it is {int} hours ago on the server', (hours: number) => {
  start.testability().backendTimeTravelRelativeToNow(-hours)
})

When('it is {int} minutes later in the browser', (minutes: number) => {
  cy.tick(minutes * 60 * 1000)
})
