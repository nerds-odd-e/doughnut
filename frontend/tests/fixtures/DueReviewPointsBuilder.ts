import Builder from "./Builder";

class RepetitionBuilder extends Builder<Generated.DueReviewPoints> {
  // eslint-disable-next-line class-methods-use-this
  do(): Generated.DueReviewPoints {
    return {
      toRepeat: [],
      dueInDays: 0,
    };
  }
}

export default RepetitionBuilder;
