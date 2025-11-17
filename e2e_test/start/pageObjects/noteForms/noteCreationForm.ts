import submittableForm from '../../submittableForm'
import { assumeWikidataSearchDialog } from '../wikidataSearchDialog'

const noteCreationForm = {
  createNoteWithTitle(title: string) {
    return submittableForm.submitWith({
      Title: title,
    })
  },

  createNoteWithTitleAndWikidataId(title: string, wikidataId: string) {
    return submittableForm
      .fill({
        Title: title,
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
