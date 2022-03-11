import Builder from "./Builder"
import ReviewPointBuilder from "./ReviewPointBuilder";

class RepetitionBuilder extends Builder<Generated.RepetitionForUser> {
  data: any

  note: any

  quizQuestion: any

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.note = null
    this.quizQuestion = undefined
  }

  ofNote(note: any): RepetitionBuilder {
    this.note = note
    return this
  }

  withAQuiz(): RepetitionBuilder {
    this.quizQuestion = {
          questionType: "CLOZE_SELECTION",
          options: [
            {
              note: {
                id: 1,
                notePicture: null,
                head: true,
                noteTypeDisplay: "Child Note",
                title: "question",
                shortDescription: "answer",
              },
              picture: false,
              display: "question",
            },
          ],
          description: "answer",
          mainTopic: "",
          pictureQuestion: false,
        }

    return this
  }

  do(): any {
    return {
        reviewPointViewedByUser: new ReviewPointBuilder().ofNote(this.note).do(),
        quizQuestion: this.quizQuestion
    }
  }
}

export default RepetitionBuilder
