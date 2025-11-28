import type { AnsweredQuestion, Note } from "@generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import makeMe from "./makeMe"

class AnsweredQuestionBuilder extends Builder<AnsweredQuestion> {
  private noteToUse?: Note
  private questionIdToUse?: number
  private isCorrect: boolean = true
  private choiceIndexToUse?: number

  withNote(note: Note): this {
    this.noteToUse = note
    return this
  }

  withQuestionId(id: number): this {
    this.questionIdToUse = id
    return this
  }

  answerCorrect(correct: boolean): this {
    this.isCorrect = correct
    return this
  }

  withChoiceIndex(index: number): this {
    this.choiceIndexToUse = index
    return this
  }

  do(): AnsweredQuestion {
    return {
      answer: {
        id: generateId(),
        correct: this.isCorrect,
        ...(this.choiceIndexToUse !== undefined && {
          choiceIndex: this.choiceIndexToUse,
        }),
      },
      answerDisplay: "",
      note: this.noteToUse ?? makeMe.aNote.please(),
      predefinedQuestion: makeMe.aPredefinedQuestion.please(),
      recallPromptId: this.questionIdToUse ?? generateId(),
    }
  }
}

export default AnsweredQuestionBuilder
