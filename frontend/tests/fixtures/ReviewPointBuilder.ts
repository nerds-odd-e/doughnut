import Builder from "./Builder";
import generateId from "./generateId";

class ReviewPointBuilder extends Builder<Generated.ReviewPoint> {
  data: Generated.ReviewPoint;

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
        createdAt: "",
      },
    };
  }

  ofNote(note: Generated.NoteRealm): ReviewPointBuilder {
    this.data.thing.note = note.note;
    return this;
  }

  ofLink(link: Generated.LinkViewedByUser): ReviewPointBuilder {
    this.data.thing.link = link.link;
    return this;
  }

  do(): Generated.ReviewPoint {
    return this.data;
  }
}

export default ReviewPointBuilder;
