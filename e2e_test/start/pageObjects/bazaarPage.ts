import { bazaarOrCircle } from './NotebookList'

export const assumeBazaarPage = () => {
  cy.findByText('Welcome To The Bazaar')

  return bazaarOrCircle()
}

export const navigateToBazaar = () => {
  cy.visit('/bazaar')

  return assumeBazaarPage()
}
