import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  expectNotebookCardButtonAbsent,
  findNotebookCardButton,
  notebookList,
} from './NotebookList'

const subscribeButtonTitle = 'Subscribe'

export const bazaarOrCircle = () => {
  return {
    ...notebookList(),
    expectCannotSubscribe(noteTopology: string) {
      expectNotebookCardButtonAbsent(noteTopology, subscribeButtonTitle)
    },
    subscribe(notebook: string, dailyAssimilationTarget: string) {
      findNotebookCardButton(notebook, subscribeButtonTitle).click()
      cy.get('#subscription-dailyTargetOfNewNotes')
        .clear()
        .type(dailyAssimilationTarget)
      cy.findByRole('button', { name: 'Submit' }).click()
      waitUntilAppIsNotBusy()
    },
  }
}
