import { form } from '../forms'
import { mainMenu } from './mainMenu'

export const navigateToMyCircles = () => {
  mainMenu()
  cy.get('.main-menu').within(() => {
    cy.findByRole('link', { name: 'Circles' }).click()
  })

  return assumeMyCirclesPage()
}

export const assumeMyCirclesPage = () => {
  cy.url().should('include', 'd/circles')
  return {
    createNewCircle: (circleName: string) => {
      cy.findByRole('button', { name: 'Create a new circle' }).click()
      form.getField('Name').type(circleName)
      cy.get('input[value="Submit"]').click()
      return assumeCirclePage(circleName)
    },
  }
}

export const assumeCirclePage = (circleName: string) => {
  cy.findByText(`Circle: ${circleName}`)
  return {
    copyInvitationCode: () => {
      cy.get('#invitation-code')
        .invoke('val')
        .then((text) => {
          cy.wrap(text).as('savedInvitationCode')
        })
    },
  }
}
