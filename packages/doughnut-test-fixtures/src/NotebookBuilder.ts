import type { Note, Notebook } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NoteBuilder from './NoteBuilder'
import generateId from './generateId'

class NotebookBuilder extends Builder<Notebook> {
  data: Notebook

  notebuilder = new NoteBuilder()

  constructor() {
    super()
    const now = new Date().toISOString()
    this.data = {
      id: generateId(),
      name: this.notebuilder.data.noteTopology.title ?? '',
      notebookSettings: {
        skipMemoryTrackingEntirely: false,
      },
      createdAt: now,
      updatedAt: now,
    }
  }

  withSeedNote(note: Note | undefined) {
    this.notebuilder.for(note)
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
    this.data.name = built.noteTopology.title ?? ''
    this.data.description = built.details
      ? `${built.details}, just shorter`
      : undefined
    return this.data
  }
}

export default NotebookBuilder
