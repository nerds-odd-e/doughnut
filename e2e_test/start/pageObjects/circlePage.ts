import { bazaarOrCircle } from './BazaarOrCircle'
import {
  clickNotebookCardTitleToOpenNotebookPage,
  findNotebookCardButton,
} from './NotebookList'
import { pageIsNotLoading } from '../pageBase'
import notebookPage from './notebookPage'
import notebookCreationForm from './forms/notebookCreationForm'

export const circleIdAlias = (circleName: string) =>
  `circleId-${circleName.replace(/\s+/g, '-')}`

const completeMoveNotebookToNewGroupDialog = (newGroupName: string) => {
  cy.findByRole('dialog', { name: 'Move to group' }).within(() => {
    cy.get('#notebook-catalog-move-to-group-target').select('new')
    cy.findByLabelText('New group name').type(newGroupName)
    cy.findByRole('button', { name: 'Move' }).click()
  })
  pageIsNotLoading()
}

export const assumeCirclePage = () => ({
  creatingNotebook(notebookTopic: string) {
    cy.findByText('Add New Notebook In This Circle', { timeout: 15000 })
      .should('be.visible')
      .click()
    cy.findByRole('textbox', { name: 'Title' }).should('be.visible')
    return notebookCreationForm.createNotebookWithNameAndDescription(
      notebookTopic,
      undefined
    )
  },
  haveMembers(count: number) {
    cy.get('body').find('.circle-member').should('have.length', count)
  },
  expectCatalogLayoutControls() {
    cy.findByRole('button', { name: 'Grid view' }).should('be.visible')
    cy.findByRole('button', { name: 'List view' }).should('be.visible')
    return this as any
  },
  creatingNotebookGroupFromCatalogMove(
    notebookName: string,
    groupName: string
  ) {
    findNotebookCardButton(notebookName, 'Move to group…').click()
    completeMoveNotebookToNewGroupDialog(groupName)
    return this as any
  },
  moveNotebook(notebookName: string) {
    clickNotebookCardTitleToOpenNotebookPage(notebookName)
    notebookPage().moveNotebookToCircle()
    return {
      toCircle(circleName: string) {
        cy.findByText(circleName).click()
        cy.findByText('OK').click()
      },
    }
  },
  ...bazaarOrCircle(),
})

export const navigateToCircle = (circleName: string) => {
  const alias = circleIdAlias(circleName)
  cy.wrap(null).then(() => {
    const aliases = Cypress.state('aliases') as
      | Record<string, unknown>
      | undefined
    if (aliases?.[alias]) {
      return cy.get(`@${alias}`, { log: false }).then((circleId) => {
        cy.visit(`/circles/${circleId}`)
      })
    }
    cy.visit('/circles')
    cy.findByText(circleName, { selector: 'a', timeout: 15000 })
      .should('be.visible')
      .click()
    return cy.url().then((url) => {
      const circleId = url.match(/\/circles\/(\d+)/)?.[1]
      if (circleId) {
        cy.wrap(circleId).as(alias)
      }
    })
  })
  pageIsNotLoading()
  cy.findByText(`Circle: ${circleName}`)
  cy.findByText('Add New Notebook In This Circle', { timeout: 15000 }).should(
    'be.visible'
  )
  return assumeCirclePage()
}
