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
    assumeWikidataSearchDialog().open().setWikidataId(wikidataId).close()
    form.submit()
  },
  wikidataSearch() {
    return assumeWikidataSearchDialog()
  },
}

export default noteCreationForm
