import submittableForm from '../../submittableForm'
import { assumeWikidataSearchDialog } from '../wikidataSearchDialog'

const noteCreationForm = {
  createNote: (title: string) => {
    submittableForm.submitWith({
      Title: title,
    })
  },
  createNoteWithWikidataId: (title: string, wikidataId?: string) => {
    submittableForm.submitWith({
      Title: title,
      'Wikidata Id': wikidataId,
    })
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
