/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given } from "@badeball/cypress-cucumber-preprocessor"

Given("I let the server to time travel to {int} hours ago", (hours: number) => {
  start.testability().backendTimeTravelRelativeToNow(-hours)
})
