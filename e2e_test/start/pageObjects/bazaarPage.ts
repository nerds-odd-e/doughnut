import { bazaarOrCircle } from "./bazaarOrCircle"

export const navigateToBazaar = () => {
  cy.visit("/bazaar")

  return bazaarOrCircle()
}
