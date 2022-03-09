import { merge } from "lodash";
import Builder from "./Builder";
import NotePositionBuilder from "./NotePositionBuilder"

let idCounter = 1;

const generateId = () => { idCounter += 1 };

class ReviewPointBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      reviewPoint: {
        id: generateId(),
      },
      noteWithPosition: null,
      linkViewedByUser: null,
    };
  }

  remainingInitialReviewCountForToday(value: number): ReviewPointBuilder {
    this.data.remainingInitialReviewCountForToday = value
    return this;
  }

  ofNote(note: any): ReviewPointBuilder {
    this.data.noteWithPosition = {
      note,
      notePosition: new NotePositionBuilder().do()
    }
    this.data.reviewPoint.noteId = note.id
    return this
  }

  ofLink(link: any): ReviewPointBuilder {
    this.data.linkViewedByUser = link
    this.data.reviewPoint.linkId = link.id
    return this
  }

  do(): any {
    return merge(
      {}, this.data
    );
  }
}

export default ReviewPointBuilder;
