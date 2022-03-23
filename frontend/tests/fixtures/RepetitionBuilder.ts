import Builder from "./Builder"
import generateId from "./generateId";
import NoteSphereBuilder from "./NoteSphereBuilder";
import ReviewPointBuilder from "./ReviewPointBuilder";

class RepetitionBuilder extends Builder<Generated.RepetitionForUser> {
  note?: Generated.NoteSphere

  quizQuestion?: Generated.QuizQuestionViewedByUser

  ofNote(note: Generated.NoteSphere): RepetitionBuilder {
    this.note = note
    return this
  }

  withAQuiz(): RepetitionBuilder {
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

    return this
  }

  do(): Generated.RepetitionForUser {
    const reviewPointBuilder = new ReviewPointBuilder()
    if(this.note) reviewPointBuilder.ofNote(this.note)
    return {
        reviewPointViewedByUser: reviewPointBuilder.do(),
        quizQuestion: this.quizQuestion,
        toRepeatCount: 0,
    }
  }
}

export default RepetitionBuilder
