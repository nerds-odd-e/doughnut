import { DueReviewPoints } from "@/generated/backend";
import Builder from "./Builder";

class RepetitionBuilder extends Builder<DueReviewPoints> {
  // eslint-disable-next-line class-methods-use-this
  do(): DueReviewPoints {
    return {
      toRepeat: [],
      dueInDays: 0,
    };
  }
}

export default RepetitionBuilder;
