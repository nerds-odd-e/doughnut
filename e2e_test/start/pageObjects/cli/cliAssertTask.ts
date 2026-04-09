import type { ManagedTtyAssertTaskPayload } from '../../../config/cliE2ePluginTasks'

export function cliAssertTask(
  body: ManagedTtyAssertTaskPayload
): Cypress.Chainable<null> {
  return cy.task<null>('cliAssert', body)
}
