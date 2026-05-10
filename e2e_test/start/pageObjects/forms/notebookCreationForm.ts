import { submittableForm } from '../../forms'

const notebookCreationForm = {
  createNotebookWithNameAndDescription(
    notebookName: string,
    description: string | undefined
  ) {
    return submittableForm.submitWith({
      Title: notebookName,
      Description: description,
    })
  },
}

export default notebookCreationForm
