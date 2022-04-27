import Builder from "./Builder"
import generateId from "./generateId";

class RepetitionBuilder extends Builder<Generated.RepetitionForUser> {
  quizQuestion: Generated.QuizQuestionViewedByUser | undefined

  reviewPointId: Doughnut.ID = 0

  withQuestion() {
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
              noteId: 1,
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
        return this;
  }

  withReviewPointId(id: Doughnut.ID) {
    this.reviewPointId = id
    return this;
  }

  do(): Generated.RepetitionForUser {
    return {
        quizQuestion: this.quizQuestion,
        reviewPoint: this.reviewPointId,
        toRepeatCount: 0,
    }
  }
}

export default RepetitionBuilder
