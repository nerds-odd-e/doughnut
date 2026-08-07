import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  expectNotebookCardButtonAbsent,
  findNotebookCardButton,
  notebookList,
} from './NotebookList'

const addToMyLearning = 'Add to my learning'

export const bazaarOrCircle = () => {
  return {
    ...notebookList(),
    expectCannotAddToMyLearning(noteTopology: string) {
      expectNotebookCardButtonAbsent(noteTopology, addToMyLearning)
    },
    subscribe(notebook: string, dailyLearningCount: string) {
      findNotebookCardButton(notebook, addToMyLearning).click()
      cy.get('#subscription-dailyTargetOfNewNotes')
        .clear()
        .type(dailyLearningCount)
      cy.findByRole('button', { name: 'Submit' }).click()
      waitUntilAppIsNotBusy()
    },
  }
}
