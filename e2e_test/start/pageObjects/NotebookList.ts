import { commonSenseSplit } from '../../support/string_util'
import { assumeAssessmentPage } from './AssessmentPage'

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
  }
}

export const bazaarOrCircle = () => {
  return {
    ...notebookList(),
    beginAssessmentOnNotebook(notebook: string) {
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
    expectCertificationIcon(notebook: string, exists: boolean) {
      const icon = cy
        .findByText(notebook)
        .parents('.card')
        .find('.certification-icon')
      if (exists) {
        icon.should('exist')
      } else {
        icon.should('not.exist')
      }
    },
  }
}
