import { assumeMemoryTrackerPage } from './memoryTrackerPage'
import { toolbarButton } from './toolbarButton'
import { questionListPage } from './questionListPage'
import { form } from 'start/forms'

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
    expectMemoryTrackerInfo(expected: { [key: string]: string }[]) {
      for (const k in expected) {
        cy.contains('tr', expected[k]?.type ?? '').within(() => {
          for (const attr in expected[k]) {
            if (expected[k][attr] !== undefined) {
              cy.contains('td', expected[k][attr])
            }
          }
        })
      }
    },
    removeMemoryTrackerFromReview(type: 'normal' | 'spelling') {
      cy.contains('tr', type).click()
      cy.url().should('include', '/d/memory-trackers/')
      cy.pageIsNotLoading()
      return assumeMemoryTrackerPage().removeFromReview()
    },
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
    editNoteUrl(attributes: Record<string, string>) {
      toolbarButton('Edit Note URL')
        .click()
        .submitWith(filterAttributes(attributes, ['Url']))
    },
    updateNoteType(noteType: string) {
      form.fill({
        'Note Type': noteType,
      })
      cy.pageIsNotLoading()
    },
    generateImageWithDALLE() {
      toolbarButton('Generate Image with DALL-E').click()
    },
    deleteNote() {
      toolbarButton('Delete note').click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.pageIsNotLoading()
    },
    openQuestionList() {
      toolbarButton('Questions for the note').click()
      return questionListPage()
    },
    assimilateNote() {
      toolbarButton('Assimilate this note').click()
      cy.url().should('include', '/d/assimilate/')
      cy.pageIsNotLoading()
      // Wait for the assimilation page to load by checking for the "Keep for repetition" button
      cy.findByRole(
        'button',
        { name: 'Keep for repetition' },
        { timeout: 10000 }
      ).should('be.visible')
    },
  }
}
