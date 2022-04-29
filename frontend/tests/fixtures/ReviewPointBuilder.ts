import Builder from "./Builder";
import generateId from "./generateId";
import NotePositionBuilder from "./NotePositionBuilder";

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
        noteId: 0,
        linkId: 0,
      },
      reviewSetting: {
        id: 0,
        rememberSpelling: false,
        level: 0,
      },
    };
  }

  ofNote(note: Generated.NoteRealm): ReviewPointBuilder {
    this.data.noteWithPosition = {
      note,
      notePosition: new NotePositionBuilder().do(),
    };
    this.data.reviewPoint.noteId = note.id;
    return this;
  }

  ofLink(link: Generated.LinkViewedByUser): ReviewPointBuilder {
    this.data.linkViewedByUser = link;
    this.data.reviewPoint.linkId = link.id;
    return this;
  }

  do(): Generated.ReviewPointViewedByUser {
    return this.data;
  }
}

export default ReviewPointBuilder;
