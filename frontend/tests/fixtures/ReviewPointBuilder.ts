import { merge } from "lodash";
import Builder from "./Builder";
import BreadcrumbBuilder from "./BreadcrumbBuilder"

let idCounter = 1;

const generateId = () => {
  return idCounter++;
};

class ReviewPointBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      reviewPoint: {
        id: generateId,
      },
      noteViewedByUser: null,
      linkViewedByUser: null,
    };
  }

  remainingInitialReviewCountForToday(value: number): ReviewPointBuilder {
    this.data.remainingInitialReviewCountForToday = value
    return this;
  }

  ofNote(note: any): ReviewPointBuilder {
    this.data.noteViewedByUser = {
      noteItself: note,
      noteBreadcrumbViewedByUser: new BreadcrumbBuilder().do()
    }
    return this
  }
  
  ofLink(link: any): ReviewPointBuilder {
    this.data.linkViewedByUser = link
    return this
  }
  
  do(): any {
    return merge(
      {}, this.data
    );
  }
}

export default ReviewPointBuilder;
