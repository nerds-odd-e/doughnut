import type { Note, Notebook } from "@/generated/backend"
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
      headNoteTopic: this.notebuilder.data.noteTopic,
      notebookSettings: {
        skipReviewEntirely: false,
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
    this.data.headNoteTopic = this.notebuilder.do().noteTopic
    return this.data
  }
}

export default NotebookBuilder
