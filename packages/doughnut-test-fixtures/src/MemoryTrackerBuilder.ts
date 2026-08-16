import type {
  Note,
  NoteRealm,
  MemoryTracker,
  RecalledNote,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NoteBuilder from './NoteBuilder'
import generateId from './generateId'

class MemoryTrackerBuilder extends Builder<MemoryTracker> {
  data: MemoryTracker
  private notebookId = generateId()
  private propertyKey?: string
  private ancestorFolders: RecalledNote['ancestorFolders'] = []

  constructor() {
    super()
    this.data = {
      id: generateId(),
      lastRecalledAt: '',
      nextRecallAt: '',
      assimilatedAt: '',
      recallCount: 0,
      stability: 0,
      removedFromTracking: false,
      note: new NoteBuilder().do(),
    }
  }

  id(value: number): MemoryTrackerBuilder {
    this.data.id = value
    return this
  }

  withPropertyKey(propertyKey: string): MemoryTrackerBuilder {
    this.propertyKey = propertyKey
    return this
  }

  assimilatedAt(assimilatedAt: string): MemoryTrackerBuilder {
    this.data.assimilatedAt = assimilatedAt
    return this
  }

  nextRecallAt(nextRecallAt: string): MemoryTrackerBuilder {
    this.data.nextRecallAt = nextRecallAt
    return this
  }

  recallCount(recallCount: number): MemoryTrackerBuilder {
    this.data.recallCount = recallCount
    return this
  }

  stability(stability: number): MemoryTrackerBuilder {
    this.data.stability = stability
    return this
  }

  difficulty(difficulty: number): MemoryTrackerBuilder {
    this.data.difficulty = difficulty
    return this
  }

  removedFromTracking(removedFromTracking: boolean): MemoryTrackerBuilder {
    this.data.removedFromTracking = removedFromTracking
    return this
  }

  spelling(value = true): MemoryTrackerBuilder {
    this.data.spelling = value
    if (value) {
      this.data.type = 'SPELLING'
    }
    return this
  }

  commissioned(): MemoryTrackerBuilder {
    this.data.type = 'COMMISSIONED'
    this.data.spelling = false
    return this
  }

  latestTutorFeedbackScore(score: number): MemoryTrackerBuilder {
    this.data.latestTutorFeedbackScore = score
    return this
  }

  ofNote(note: NoteRealm): MemoryTrackerBuilder {
    this.data.note = note.note
    this.notebookId = note.notebookRealm.notebook.id
    this.ancestorFolders = note.ancestorFolders ?? []
    return this
  }

  ofLink(link: Note): MemoryTrackerBuilder {
    this.data.note = link
    return this
  }

  private buildRecalledNote(): RecalledNote {
    const note = this.data.note
    return {
      noteTopology: note.noteTopology,
      notebookId: this.notebookId,
      ancestorFolders: this.ancestorFolders ?? [],
      propertyKey: this.propertyKey,
    }
  }

  do(): MemoryTracker {
    this.data.recalledNote = this.buildRecalledNote()
    if (this.propertyKey !== undefined) {
      this.data.propertyKey = this.propertyKey
    }
    return this.data
  }
}

export default MemoryTrackerBuilder
