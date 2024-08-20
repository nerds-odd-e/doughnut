import { bazaarOrCircle } from './NotebookList'

export const assumeBazaarPage = () => {
  cy.findByText('Welcome To The Bazaar')

  return bazaarOrCircle()
}

export const navigateToBazaar = () => {
  cy.visit('/bazaar')
  cy.get('h2', { timeout: 3000 }).should('contain', 'Welcome To The Bazaar')

  return assumeBazaarPage()
}
