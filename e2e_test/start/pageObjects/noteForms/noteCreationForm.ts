import submittableForm from '../../submittableForm'
import { assumeWikidataSearchDialog } from '../wikidataSearchDialog'

const noteCreationForm = {
  createNoteWithTitle(title: string) {
    return submittableForm.submitWith({
      Title: title,
    })
  },

  createNoteWithTitleAndWikidataId(title: string, wikidataId: string) {
    const form = submittableForm.fill({
      Title: title,
    })
    // Open the dialog and fill the Wikidata ID input
    cy.findByRole('button', { name: 'Wikidata Id' }).click()
    cy.findByText('Search Wikidata').should('be.visible')
    cy.get('.modal-container').within(() => {
      cy.formField('Wikidata Id').assignFieldValue(wikidataId)
    })
    // Close the dialog
    cy.get('.modal-container').within(() => {
      cy.findByRole('button', { name: 'Cancel' }).click()
    })
    form.submit()
  },
  wikidataSearch() {
    return assumeWikidataSearchDialog()
  },
}

export default noteCreationForm
