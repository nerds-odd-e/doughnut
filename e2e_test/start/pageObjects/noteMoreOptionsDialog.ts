import { pageIsNotLoading } from '../pageBase'
import { assumeAssimilationPage } from './assimilationPage'
import { toolbarButton } from './toolbarButton'
import { questionListPage } from './questionListPage'

function filterAttributes(
  attributes: Record<string, string>,
  keysToKeep: string[]
) {
  return Object.keys(attributes)
    .filter((key) => keysToKeep.includes(key))
    .reduce(
      (obj, key) => {
        const val = attributes[key]
        if (val) {
          obj[key] = val
        }
        return obj
      },
      {} as Record<string, string>
    )
}

export const makeSureNoteMoreOptionsDialogIsOpen = () => {
  cy.findByRole('button', { name: 'more options' }).then(($button) => {
    if (!$button.hasClass('daisy-btn-active')) {
      cy.wrap($button).click()
    }
  })

  return noteMoreOptionsDialog()
}

const noteMoreOptionsDialog = () => {
  return {
    toolbarButton,
    editNoteImage(attributes: Record<string, string>) {
      toolbarButton('Edit Note Image')
        .click()
        .submitWith(
          filterAttributes(attributes, [
            'Upload Image',
            'Image Url',
            'Use Parent Image',
          ])
        )
    },
    deleteNote() {
      toolbarButton('Delete note').click()
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
    },
    openQuestionList() {
      toolbarButton('Questions for the note').click()
      return questionListPage()
    },
    openAssimilationPage() {
      toolbarButton('Assimilation settings').click()
      cy.url().should('include', '/d/assimilate/')
      pageIsNotLoading()
      return assumeAssimilationPage().waitForAssimilationReady()
    },
    assimilateNote() {
      this.openAssimilationPage().clickKeepForRecall()
      pageIsNotLoading()
    },
  }
}
