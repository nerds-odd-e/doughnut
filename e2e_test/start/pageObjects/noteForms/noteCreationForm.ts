import submittableForm from '../../submittableForm'
import { assumeWikidataSearchDialog } from '../wikidataSearchDialog'

const noteCreationForm = {
  createNote: (title: string) => {
    submittableForm.submitWith({
      Title: title,
    })
  },
  createNoteWithWikidataId: (title: string, wikidataId?: string) => {
    cy.formField('Title').assignFieldValue(title)
    if (wikidataId) {
      cy.findByRole('button', { name: 'Wikidata Id' }).click()
      cy.findByText('Search Wikidata').should('be.visible')
      cy.findByText('Search Wikidata')
        .closest('.modal-container')
        .within(() => {
          cy.formField('Wikidata Id').clear().type(wikidataId)
          // Trigger blur to ensure the value is saved
          cy.formField('Wikidata Id').blur()
        })
      // Close dialog by clicking the close button (X) - this should preserve the value
      cy.get('.close-button').click()
      // Wait for dialog to close
      cy.findByText('Search Wikidata').should('not.exist')
    }
    cy.get('input[value="Submit"]').click()
    cy.pageIsNotLoading()
  },
  createNoteWithAttributes(attributes: Record<string, string>) {
    const { Title, 'Wikidata Id': wikidataId, ...remainingAttrs } = attributes
    expect(Object.keys(remainingAttrs)).to.have.lengthOf(0)
    return this.createNoteWithWikidataId(Title!, wikidataId)
  },
  wikidataSearch() {
    return assumeWikidataSearchDialog()
  },
}

export default noteCreationForm
