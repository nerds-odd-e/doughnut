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
    this.wikidataSearch().setWikidataId(wikidataId).close()
    form.submit()
  },
  wikidataSearch() {
    cy.findByRole('button', { name: 'Wikidata Id' }).click()
    return assumeWikidataSearchDialog()
  },
  searchWikidata(phrase: string) {
    cy.formField('Title').assignFieldValue(phrase)
    return this.wikidataSearch()
  },
}

export default noteCreationForm
