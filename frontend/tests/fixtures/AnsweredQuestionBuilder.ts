import type { AnsweredQuestion, Note } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import makeMe from "./makeMe"

class AnsweredQuestionBuilder extends Builder<AnsweredQuestion> {
  private noteToUse?: Note
  private reviewQuestionInstanceIdToUse?: number

  withNote(note: Note): this {
    this.noteToUse = note
    return this
  }

  withReviewQuestionInstanceId(id: number): this {
    this.reviewQuestionInstanceIdToUse = id
    return this
  }

  do(): AnsweredQuestion {
    return {
      answer: {
        id: generateId(),
        correct: true,
      },
      answerDisplay: "",
      note: this.noteToUse ?? makeMe.aNote.please(),
      predefinedQuestion: makeMe.aPredefinedQuestion.please(),
      reviewQuestionInstanceId:
        this.reviewQuestionInstanceIdToUse ?? generateId(),
    }
  }
}

export default AnsweredQuestionBuilder
