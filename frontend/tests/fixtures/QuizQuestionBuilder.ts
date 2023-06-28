import Builder from "./Builder";
import generateId from "./generateId";

class QuizQuestionBuilder extends Builder<Generated.QuizQuestion> {
  quizQuestion: Generated.QuizQuestion = {
    reviewPointId: 0,
    rawJsonQuestion: "",
    quizQuestion: {
      id: generateId(),
      reviewPoint: 0,
      questionTypeId: 0,
      categoryLink: 0,
      viceReviewPointIds: "",
      createdAt: "",
      optionThingIds: "",
      rawJsonQuestion: "",
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

  withClozeSelectionQuestion() {
    return this.withQuestionType("CLOZE_SELECTION");
  }

  withQuestionType(questionType: Generated.QuestionType) {
    this.quizQuestion.questionType = questionType;
    return this;
  }

  withReviewPointId(id: Doughnut.ID) {
    this.quizQuestion.quizQuestion.reviewPoint = id;
    return this;
  }

  withRawJsonQuestion(json: string) {
    this.quizQuestion.quizQuestion.rawJsonQuestion = json;
    return this;
  }

  do(): Generated.QuizQuestion {
    return this.quizQuestion;
  }
}

export default QuizQuestionBuilder;
