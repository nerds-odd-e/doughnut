import { Note, Notebook } from "@/generated/backend"
import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder"
import generateId from "./generateId"

class NotebookBuilder extends Builder<Notebook> {
  data: Notebook

  notebuilder = new NoteBuilder()

  constructor() {
    super()
    this.data = {
      id: generateId(),
      headNote: this.notebuilder.data,
      notebookSettings: {
        skipReviewEntirely: false,
      },
    }
  }

  headNote(headNote: Note | undefined) {
    this.notebuilder.for(headNote)
    return this
  }

  numberOfQuestionsInAssessment(numberofQuestions: number) {
    this.data.notebookSettings.numberOfQuestionsInAssessment = numberofQuestions
    return this
  }

  do(): Notebook {
    this.data.headNote = this.notebuilder.do()
    return this.data
  }
}

export default NotebookBuilder
