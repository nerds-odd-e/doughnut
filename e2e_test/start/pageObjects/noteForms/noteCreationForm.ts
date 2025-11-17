import submittableForm from '../../submittableForm'
import { assumeWikidataSearchDialog } from '../wikidataSearchDialog'

const noteCreationForm = {
  createNote: (title: string) => {
    submittableForm.submitWith({
      Title: title,
    })
  },

  createNoteWithAttributes(attributes: Record<string, string>) {
    const { Title, 'Wikidata Id': wikidataId, ...remainingAttrs } = attributes
    expect(Object.keys(remainingAttrs)).to.have.lengthOf(0)
    return submittableForm
      .fill({
        Title,
      })
      .fill({
        'Wikidata Id': wikidataId,
      })
      .submit()
  },
  wikidataSearch() {
    return assumeWikidataSearchDialog()
  },
}

export default noteCreationForm
