export const basicDialog = {
  assumeDialogIsOpen: () => {
    return {
      close: () => {
        cy.get("button.close-button").click()
      },
    }
  },
}
