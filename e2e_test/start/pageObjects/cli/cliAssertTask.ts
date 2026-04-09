import type { ManagedTtyAssertInput } from 'tty-assert'

export function cliAssertTask(
  body: ManagedTtyAssertInput
): Cypress.Chainable<null> {
  return cy.task<null>('cliAssert', body)
}
