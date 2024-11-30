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
      lastReviewedAt: "",
      nextReviewAt: "",
      initialReviewedAt: "",
      repetitionCount: 0,
      forgettingCurveIndex: 0,
      removedFromReview: false,
      note: new NoteBuilder().do(),
    }
  }

  initialReviewedAt(initialReviewedAt: string): MemoryTrackerBuilder {
    this.data.initialReviewedAt = initialReviewedAt
    return this
  }

  nextReviewAt(nextReviewAt: string): MemoryTrackerBuilder {
    this.data.nextReviewAt = nextReviewAt
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

  removedFromReview(removedFromReview: boolean): MemoryTrackerBuilder {
    this.data.removedFromReview = removedFromReview
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
