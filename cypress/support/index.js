// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

/// <reference types="cypress" />

// Import commands.js using ES2015 syntax:
import "./commands";
import "./testability";

//
// When the active polling features are added, the browser page is still polling when the next test started.
// but the session is already reset, so this cause exception. This is to suppress the exceptions temporarily
//
// Remove this before: 12 Feb 2022
//
Cypress.on('uncaught:exception', (err, runnable) => {
  return false
})

String.prototype.commonSenseSplit = function (separator) {
  return this.trim()
    .split(separator)
    .filter((s) => s !== "")
    .map((s) => s.trim());
};

// Alternatively you can use CommonJS syntax:
// require('./commands')
