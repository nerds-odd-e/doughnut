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
    form.fill({
      'Wikidata Id': wikidataId,
    })
    form.submit()
  },
  wikidataSearch() {
    return assumeWikidataSearchDialog()
  },
}

export default noteCreationForm
