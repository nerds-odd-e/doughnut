/**
 * daisyUI v5 native `<dialog class="daisy-modal">`: `daisy-modal-open` toggles app state but
 * the UA keeps `display: none` until `showModal()`. Cypress often cannot open the dialog, so
 * interact with controls using `{ force: true }` on the open dialog subtree.
 *
 * `Modal.vue` popups (`dialog.modal-mask.popups`) are visible without `[open]`; target the
 * visible dialog for OK clicks.
 */

export function openDaisyDialog(selector: string): void {
  cy.get(`${selector}.daisy-modal-open`, { timeout: 10000 }).should('exist')
}

/** `usePopups().confirm()` via Modal.vue */
export function clickPopupConfirmOk(): void {
  cy.get('dialog').filter(':visible').contains('button', 'OK').click()
}

/** Decline `usePopups().confirm()` when a merge prompt is shown. */
export function declineMergeConfirmIfShown(): void {
  cy.get('dialog', { timeout: 10000 })
    .filter(':visible')
    .then(($dialogs) => {
      const mergeDialog = [...$dialogs].find((el) =>
        el.textContent?.includes('Merge into it?')
      )
      if (mergeDialog) {
        cy.wrap(mergeDialog).contains('button', 'Cancel').click()
      }
    })
}

export function clickDaisyDialogButton(
  dialogSelector: string,
  buttonName: string
): void {
  openDaisyDialog(dialogSelector)
  cy.get(`${dialogSelector}.daisy-modal-open`)
    .find('.daisy-modal-action')
    .contains('button', buttonName)
    .click({ force: true })
}

export function expectDaisyDialogBoxVisible(dialogSelector: string): void {
  openDaisyDialog(dialogSelector)
  cy.get(`${dialogSelector}.daisy-modal-open .daisy-modal-box`).should('exist')
}
