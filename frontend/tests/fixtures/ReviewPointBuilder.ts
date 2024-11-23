import type { Note, NoteRealm, ReviewPoint } from "@/generated/backend"
import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder"
import generateId from "./generateId"

class ReviewPointBuilder extends Builder<ReviewPoint> {
  data: ReviewPoint

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

  do(): ReviewPoint {
    return this.data
  }
}

export default ReviewPointBuilder
