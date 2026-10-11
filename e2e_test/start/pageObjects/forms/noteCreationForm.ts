import { form, submittableForm } from '../../forms'
import { assumeAssociateWikidataDialog } from '../associateWikidataDialog'
import { arrivalMarker } from '../dictationArrivalMarker'

const newNoteForm = () => cy.findByTestId('note-new-form')

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

  speakTheTitle() {
    cy.findByRole('button', { name: 'Speak the title' }).click()
    return this
  },

  stopSpeakingTheTitle() {
    cy.findByRole('button', { name: 'Stop speaking the title' }).click()
    return this
  },

  expectArrivalPlaceMarkedInTitle() {
    newNoteForm().find(arrivalMarker).should('be.visible')
    return this
  },

  expectNoArrivalPlaceMarkedInTitle() {
    newNoteForm().find(arrivalMarker).should('not.exist')
    return this
  },

  expectTitle(title: string) {
    form.getField('Title').shouldHaveValue(title)
    return this
  },

  selectParentRelationship(label: string) {
    cy.findByTestId('note-creation-parent-relationship')
      .contains('label', label)
      .click()
    return this
  },

  createNoteWithTitleAndParentRelationship(
    title: string,
    relationship: string
  ) {
    this.selectParentRelationship(relationship)
    return this.createNoteWithTitle(title)
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
