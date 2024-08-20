import { commonSenseSplit } from '../../support/string_util'
import { assumeAssessmentPage } from './AssessmentPage'
import { expectCertificationIcon } from './bazaarPage'

const addToMyLearning = 'Add to my learning'

export const notebookList = () => {
  cy.pageIsNotLoading()
  return {
    expectNotebooks: (notebooks: string) => {
      cy.pageIsNotLoading()
      cy.get('h5 .topic-text').then(($els) => {
        const cardTitles = Array.from($els, (el) => el.innerText)
        expect(cardTitles).to.deep.eq(commonSenseSplit(notebooks, ','))
      })
    },
    findNotebookCardButton: (notebook: string, name: string) => {
      const finder = () =>
        cy
          .findCardTitle(notebook)
          .parent()
          .parent()
          .parent()
          .parent()
          .parent()
          .findByRole('button', { name: name })

      return {
        click() {
          finder().click()
        },
        shouldNotExist() {
          finder().should('not.exist')
        },
      }
    },
    notebookAssistant(notebook: string) {
      this.findNotebookCardButton(notebook, 'Notebook Assistant').click()
      return {
        create(instruction: string) {
          cy.formField('Additional Instruction').type(instruction)
          cy.findByRole('button', {
            name: 'Create Assistant For Notebook',
          }).click()
          cy.pageIsNotLoading()
        },
      }
    },
  }
}

export const bazaarOrCircle = () => {
  return {
    ...notebookList(),
    selfAssessmentOnNotebook(notebook: string) {
      this.findNotebookCardButton(notebook, 'Start Assessment').click()
      return assumeAssessmentPage()
    },
    expectNoAddToMyLearningButton(noteTopic: string) {
      this.findNotebookCardButton(noteTopic, addToMyLearning).shouldNotExist()
    },
    subscribe(notebook: string, dailyLearningCount: string) {
      this.findNotebookCardButton(notebook, addToMyLearning).click()
      cy.get('#subscription-dailyTargetOfNewNotes')
        .clear()
        .type(dailyLearningCount)
      cy.findByRole('button', { name: 'Submit' }).click()
    },
    expectCertificationIcon(notebook: string) {
      cy.findByText(notebook)
        .parents('.card')
        .find('.btn')
        .should('exist')
    }
  }
}
