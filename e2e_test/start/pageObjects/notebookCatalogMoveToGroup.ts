import { waitUntilAppIsNotBusy } from '../pageBase'

/** Completes the Move-to-group dialog by creating a new group and waits for it in the catalog. */
export const completeMoveNotebookToNewGroupDialog = (newGroupName: string) => {
  cy.findByRole('dialog', { name: 'Move to group' }).within(() => {
    cy.get('#notebook-catalog-move-to-group-target').select('new')
    cy.findByLabelText('New group name').type(newGroupName, { delay: 0 })
    cy.findByRole('button', { name: 'Move' }).click()
  })
  waitUntilAppIsNotBusy()
  // Move success refreshes the catalog without data-app-busy; wait for the group to appear.
  cy.contains('[data-cy="notebook-group-card"]', newGroupName, {
    timeout: 15000,
  }).should('be.visible')
}

export const completeMoveNotebookToUngroupedDialog = () => {
  cy.findByRole('dialog', { name: 'Move to group' }).within(() => {
    cy.get('#notebook-catalog-move-to-group-target').select('ungrouped')
    cy.findByRole('button', { name: 'Move' }).click()
  })
  waitUntilAppIsNotBusy()
}
