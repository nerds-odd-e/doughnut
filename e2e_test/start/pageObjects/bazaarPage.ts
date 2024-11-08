import { bazaarOrCircle } from './BazaarOrCircle'

export const assumeBazaarPage = () => {
  cy.findByText('Welcome To The Bazaar')

  return bazaarOrCircle()
}

export const navigateToBazaar = () => {
  cy.visit('/d/bazaar')
  cy.get('h2', { timeout: 3000 }).should('contain', 'Welcome To The Bazaar')

  return assumeBazaarPage()
}
