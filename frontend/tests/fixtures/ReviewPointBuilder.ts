import { Thing } from "@/generated/backend/models/Thing";
import { NoteRealm, ReviewPoint } from "@/generated/backend";
import Builder from "./Builder";
import generateId from "./generateId";

class ReviewPointBuilder extends Builder<ReviewPoint> {
  data: ReviewPoint;

  constructor() {
    super();
    this.data = {
      id: generateId(),
      lastReviewedAt: "",
      nextReviewAt: "",
      initialReviewedAt: "",
      repetitionCount: 0,
      forgettingCurveIndex: 0,
      removedFromReview: false,
      thing: {
        id: generateId(),
      },
    };
  }

  ofNote(note: NoteRealm): ReviewPointBuilder {
    this.data.thing.note = note.note;
    return this;
  }

  ofLink(link: Thing): ReviewPointBuilder {
    this.data.thing = link;
    return this;
  }

  do(): ReviewPoint {
    return this.data;
  }
}

export default ReviewPointBuilder;
