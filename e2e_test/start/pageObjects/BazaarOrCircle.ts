import { pageIsNotLoading } from '../pageBase'
import { findNotebookCardButton, notebookList } from './NotebookList'

const addToMyLearning = 'Add to my learning'

export const bazaarOrCircle = () => {
  return {
    ...notebookList(),
    expectNoAddToMyLearningButton(noteTopology: string) {
      findNotebookCardButton(noteTopology, addToMyLearning).shouldNotExist()
    },
    subscribe(notebook: string, dailyLearningCount: string) {
      findNotebookCardButton(notebook, addToMyLearning).click()
      cy.get('#subscription-dailyTargetOfNewNotes')
        .clear()
        .type(dailyLearningCount)
      cy.findByRole('button', { name: 'Submit' }).click()
      pageIsNotLoading()
    },
  }
}
