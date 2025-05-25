import type { Note, Notebook } from "generated/backend"
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
      headNoteId: this.notebuilder.data.noteTopology.id,
      title: this.notebuilder.data.noteTopology.titleOrPredicate,
      notebookSettings: {
        skipMemoryTrackingEntirely: false,
      },
      updated_at: new Date().toISOString(),
    }
  }

  headNote(headNote: Note | undefined) {
    this.notebuilder.for(headNote)
    return this
  }

  creator(creatorId: string) {
    this.data.creatorId = creatorId
    return this
  }

  numberOfQuestionsInAssessment(numberofQuestions: number) {
    this.data.notebookSettings.numberOfQuestionsInAssessment = numberofQuestions
    return this
  }

  do(): Notebook {
    this.data.headNoteId = this.notebuilder.do().noteTopology.id
    this.data.title = this.notebuilder.do().noteTopology.titleOrPredicate
    this.data.shortDetails = this.notebuilder.do().noteTopology.shortDetails
    return this.data
  }
}

export default NotebookBuilder
