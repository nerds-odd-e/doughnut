import { bazaarOrCircle } from './BazaarOrCircle'
import router from '../router'

export const assumeBazaarPage = () => {
  cy.findByText('Welcome To The Bazaar')

  return bazaarOrCircle()
}

export const navigateToBazaar = () => {
  router().visitNamed('bazaar')
  cy.get('h2', { timeout: 3000 }).should('contain', 'Welcome To The Bazaar')

  return assumeBazaarPage()
}
