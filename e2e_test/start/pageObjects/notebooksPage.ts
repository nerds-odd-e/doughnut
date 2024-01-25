export const routerToNotebooksPage = () => {
  cy.routerPush("/notebooks", "notebooks", {})
  return {}
}
