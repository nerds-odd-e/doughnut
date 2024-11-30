import type { Note, NoteRealm, MemoryTracker } from "@/generated/backend"
import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder"
import generateId from "./generateId"

class ReviewPointBuilder extends Builder<MemoryTracker> {
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

  initialReviewedAt(initialReviewedAt: string): ReviewPointBuilder {
    this.data.initialReviewedAt = initialReviewedAt
    return this
  }

  nextReviewAt(nextReviewAt: string): ReviewPointBuilder {
    this.data.nextReviewAt = nextReviewAt
    return this
  }

  repetitionCount(repetitionCount: number): ReviewPointBuilder {
    this.data.repetitionCount = repetitionCount
    return this
  }

  forgettingCurveIndex(forgettingCurveIndex: number): ReviewPointBuilder {
    this.data.forgettingCurveIndex = forgettingCurveIndex
    return this
  }

  removedFromReview(removedFromReview: boolean): ReviewPointBuilder {
    this.data.removedFromReview = removedFromReview
    return this
  }

  ofNote(note: NoteRealm): ReviewPointBuilder {
    this.data.note = note.note
    return this
  }

  ofLink(link: Note): ReviewPointBuilder {
    this.data.note = link
    return this
  }

  do(): MemoryTracker {
    return this.data
  }
}

export default ReviewPointBuilder
