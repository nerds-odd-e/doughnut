import Builder from "./Builder"
import generateId from "./generateId";
import NoteSphereBuilder from "./NoteSphereBuilder";

class RepetitionBuilder extends Builder<Generated.RepetitionForUser> {
  quizQuestion: Generated.QuizQuestionViewedByUser

  constructor() {
    super();
    this.quizQuestion = {
          quizQuestion: {
            id: generateId(),
            reviewPoint: 0,
            questionTypeId: 0,
            categoryLink: 0,
            optionNoteIds: "",
            viceReviewPointIds: "",
            createdAt: "",
          },
          questionType: "CLOZE_SELECTION",
          options: [
            {
              note: new NoteSphereBuilder().do(),
              picture: false,
              display: "question",
            },
          ],
          viceReviewPointIdList: [],
          description: "answer",
          mainTopic: "",
          hintLinks: {},
          scope: []
        }

  }

  quizType(value: Generated.QuestionType): RepetitionBuilder {
    this.quizQuestion.questionType = value;
    this.quizQuestion.revealedNoteId = 0;
    return this;
  }

  do(): Generated.RepetitionForUser {
    return {
        quizQuestion: this.quizQuestion,
        toRepeatCount: 0,
    }
  }
}

export default RepetitionBuilder
