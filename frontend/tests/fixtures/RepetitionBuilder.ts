import Builder from "./Builder";
import generateId from "./generateId";

class RepetitionBuilder extends Builder<Generated.DueReviewPoints> {
  quizQuestion: Generated.QuizQuestionViewedByUser = {
    quizQuestion: {
      id: generateId(),
      reviewPoint: 0,
      questionTypeId: 0,
      categoryLink: 0,
      viceReviewPointIds: "",
      createdAt: "",
      optionThingIds: "",
    },
    questionType: "JUST_REVIEW",
    options: [
      {
        noteId: 1,
        picture: false,
        display: "question",
      },
    ],
    viceReviewPointIdList: [],
    description: "answer",
    mainTopic: "",
    hintLinks: {
      links: {},
    },
  };

  withQuestion() {
    this.quizQuestion.questionType = "CLOZE_SELECTION";
    return this;
  }

  withReviewPointId(id: Doughnut.ID) {
    this.quizQuestion.quizQuestion.reviewPoint = id;
    return this;
  }

  do(): Generated.DueReviewPoints {
    return {
      quizQuestion: this.quizQuestion,
      toRepeat: [],
    };
  }
}

export default RepetitionBuilder;
