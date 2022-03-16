import Builder from "./Builder"
import NoteSphereBuilder from "./NoteSphereBuilder";
import ReviewPointBuilder from "./ReviewPointBuilder";

class RepetitionBuilder extends Builder<Generated.RepetitionForUser> {
  note?: Generated.NoteSphere

  quizQuestion?: Generated.QuizQuestion

  ofNote(note: Generated.NoteSphere): RepetitionBuilder {
    this.note = note
    return this
  }

  withAQuiz(): RepetitionBuilder {
    this.quizQuestion = {
          questionType: "CLOZE_SELECTION",
          options: [
            {
              note: new NoteSphereBuilder().do(),
              picture: false,
              display: "question",
            },
          ],
          description: "answer",
          mainTopic: "",
          hintLinks: {},
          viceReviewPointIds: [],
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
        emptyAnswer: {
          answer: '',
          answerNoteId: 0,
          questionType: 'CLOZE_SELECTION',
          viceReviewPointIds: []
        },
        toRepeatCount: 0,
    }
  }
}

export default RepetitionBuilder
