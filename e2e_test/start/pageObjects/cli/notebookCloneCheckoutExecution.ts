import type { CliNotebookCloneDestinationAlias } from './notebookCloneCheckoutDestination'

export function runInstalledOn(
  destinationAlias: CliNotebookCloneDestinationAlias,
  subcommand: 'publish' | 'pull' | 'diagnose' | 'recover',
  task: 'runInstalledCli' | 'runInstalledCliExpectingRejection',
  extraArgs: string[] = []
): Cypress.Chainable<null> {
  return cy.get<string>(`@${destinationAlias}`).then((checkoutDir) =>
    cy.get<string>('@donutPath').then((donutPath) =>
      cy.get<string>('@cliConfigDir').then((configDir) =>
        cy.task<null>(task, {
          donutPath,
          args: ['notebook', subcommand, checkoutDir, ...extraArgs],
          env: { DONUT_CONFIG_DIR: configDir },
        })
      )
    )
  )
}
