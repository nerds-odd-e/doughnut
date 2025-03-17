import { findNotebookCardButton, notebookList } from './NotebookList'
import { assumeAssessmentPage } from './AssessmentPage'

const addToMyLearning = 'Add to my learning'

export const bazaarOrCircle = () => {
  return {
    ...notebookList(),
    beginAssessmentOnNotebook(notebook: string) {
      findNotebookCardButton(notebook, 'Start Assessment').click()
      return assumeAssessmentPage()
    },
    expectNoAddToMyLearningButton(noteTopology: string) {
      findNotebookCardButton(noteTopology, addToMyLearning).shouldNotExist()
    },
    subscribe(notebook: string, dailyLearningCount: string) {
      findNotebookCardButton(notebook, addToMyLearning).click()
      cy.get('#subscription-dailyTargetOfNewNotes')
        .clear()
        .type(dailyLearningCount)
      cy.findByRole('button', { name: 'Submit' }).click()
    },
    expectCertificationIcon(notebook: string, exists: boolean) {
      const icon = cy
        .findByText(notebook)
        .parents('.daisy-card')
        .find('.certification-icon')
      if (exists) {
        icon.should('exist')
      } else {
        icon.should('not.exist')
      }
    },
  }
}
