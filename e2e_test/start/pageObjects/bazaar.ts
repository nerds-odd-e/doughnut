import { bazaarOrCircle } from "./bazaarOrCircle"

export const bazaar = () => {
  cy.visit("/bazaar")

  return bazaarOrCircle()
}
