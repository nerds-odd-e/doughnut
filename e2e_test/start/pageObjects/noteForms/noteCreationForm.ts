import { form, submittableForm } from '../../forms'
import { assumeAssociateWikidataDialog } from '../associateWikidataDialog'

const noteCreationForm = {
  expectFormVisible() {
    cy.findByLabelText('Title').should('be.visible')
  },
  expectPrefilledTitle(title: string) {
    cy.findByLabelText('Title').should('have.value', title)
  },
  submit() {
    submittableForm.submit()
  },
  createNoteWithTitle(title: string) {
    return submittableForm.submitWith({
      Title: title,
    })
  },

  createNoteWithTitleAndWikidataId(title: string, wikidataId: string) {
    const form = submittableForm.fill({
      Title: title,
    })
    this.wikidataSearch().setWikidataId(wikidataId).close()
    form.submit()
  },
  wikidataSearch() {
    cy.findByRole('button', { name: 'Wikidata Id' }).click()
    return assumeAssociateWikidataDialog()
  },
  searchWikidata(phrase: string) {
    form.getField('Title').assignValue(phrase)
    return this.wikidataSearch()
  },
}

export default noteCreationForm
