/// <reference types="Cypress" />
// @ts-check
import { NotebookGitTestabilityController } from '@generated/donut-backend-api/sdk.gen'

/**
 * Test-only setup helper: rebuilds a notebook's `NotebookGitBinding` from its *current*
 * database content after a fixture seeds unsupported structural changes such as folders or
 * readmes. It establishes the fixture's initial accepted baseline; do not use it after the
 * content edit under test.
 */
export const notebookGitTestabilityMethods = {
  resnapshotNotebookGitBindingForTestability(notebookName: string) {
    return cy.wrap(
      NotebookGitTestabilityController.resnapshotNotebookGitBindingForTestability(
        {
          body: { notebookName },
        }
      ),
      { log: false }
    )
  },
}
