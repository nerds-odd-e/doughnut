import type { Note, NoteRealm, MemoryTracker } from "@/generated/backend"
import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder"
import generateId from "./generateId"

class MemoryTrackerBuilder extends Builder<MemoryTracker> {
  data: MemoryTracker

  constructor() {
    super()
    this.data = {
      id: generateId(),
      lastRecalledAt: "",
      nextRecallAt: "",
      assimilatedAt: "",
      repetitionCount: 0,
      forgettingCurveIndex: 0,
      removedFromTracking: false,
      note: new NoteBuilder().do(),
    }
  }

  assimilatedAt(assimilatedAt: string): MemoryTrackerBuilder {
    this.data.assimilatedAt = assimilatedAt
    return this
  }

  nextRecallAt(nextRecallAt: string): MemoryTrackerBuilder {
    this.data.nextRecallAt = nextRecallAt
    return this
  }

  repetitionCount(repetitionCount: number): MemoryTrackerBuilder {
    this.data.repetitionCount = repetitionCount
    return this
  }

  forgettingCurveIndex(forgettingCurveIndex: number): MemoryTrackerBuilder {
    this.data.forgettingCurveIndex = forgettingCurveIndex
    return this
  }

  removedFromTracking(removedFromTracking: boolean): MemoryTrackerBuilder {
    this.data.removedFromTracking = removedFromTracking
    return this
  }

  ofNote(note: NoteRealm): MemoryTrackerBuilder {
    this.data.note = note.note
    return this
  }

  ofLink(link: Note): MemoryTrackerBuilder {
    this.data.note = link
    return this
  }

  do(): MemoryTracker {
    return this.data
  }
}

export default MemoryTrackerBuilder
