import type { Note, Notebook } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NoteBuilder from './NoteBuilder'
import generateId from './generateId'

class NotebookBuilder extends Builder<Notebook> {
  data: Notebook

  notebuilder = new NoteBuilder()

  constructor() {
    super()
    this.data = {
      id: generateId(),
      title: this.notebuilder.data.noteTopology.title ?? '',
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
    const built = this.notebuilder.do()
    this.data.title = built.noteTopology.title ?? ''
    this.data.description = built.noteTopology.shortDetails
    return this.data
  }
}

export default NotebookBuilder
