import { bazaarOrCircle } from './NotebookList'

export const navigateToBazaar = () => {
  cy.visit('/bazaar')

  return bazaarOrCircle()
}
