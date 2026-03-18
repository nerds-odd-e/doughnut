/**
 * CLI setup page object. Domain: test lifecycle (config dir, aliases).
 * Used by Before/After hooks for CLI scenarios.
 */
export function setup() {
  return {
    createConfigDir() {
      cy.task('createCliConfigDir').as('cliConfigDir')
    },
  }
}
