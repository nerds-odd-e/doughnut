/// <reference types="Cypress" />
// @ts-check
import { NotebookGitTestabilityController } from '@generated/donut-backend-api/sdk.gen'

/**
 * Transitional, test-only: rebuilds a notebook's `NotebookGitBinding` from its *current*
 * database content, simulating a genuinely pre-cutover notebook whose binding captured
 * content that already existed at cutover time. Flagged for removal once SEED-009's Story 3
 * keeps bindings in sync with web edits automatically (see plan Slice 13).
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
