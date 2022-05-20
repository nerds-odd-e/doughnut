import Builder from "./Builder";
import generateId from "./generateId";

class ReviewPointBuilder extends Builder<Generated.ReviewPointViewedByUser> {
  data: Generated.ReviewPointViewedByUser;

  constructor() {
    super();
    this.data = {
      reviewPoint: {
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
      },
      reviewSetting: {
        id: 0,
        rememberSpelling: false,
        level: 0,
      },
    };
  }

  ofNote(note: Generated.NoteRealm): ReviewPointBuilder {
    this.data.reviewPoint.thing.note = note.note;
    return this;
  }

  ofLink(link: Generated.LinkViewedByUser): ReviewPointBuilder {
    this.data.reviewPoint.thing.link = link.link;
    return this;
  }

  do(): Generated.ReviewPointViewedByUser {
    return this.data;
  }
}

export default ReviewPointBuilder;
