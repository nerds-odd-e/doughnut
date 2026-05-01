import { form, submittableForm } from '../../forms'
import { assumeAssociateWikidataDialog } from '../associateWikidataDialog'

const noteCreationForm = {
  submit() {
    submittableForm.submit()
  },
  createFolderWithName(name: string) {
    return submittableForm.submitWith({
      'Folder name': name,
    })
  },

  createNoteWithTitle(title: string) {
    return submittableForm.submitWith({
      Title: title,
    })
  },

  createNotebookWithNameAndDescription(
    notebookName: string,
    description: string
  ) {
    return submittableForm.submitWith({
      Title: notebookName,
      Description: description,
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
