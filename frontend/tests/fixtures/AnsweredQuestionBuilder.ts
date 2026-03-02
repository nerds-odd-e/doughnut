import type { AnsweredQuestion, Note, RecallPrompt } from "@generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import makeMe from "./makeMe"
import RecallPromptBuilder from "./RecallPromptBuilder"

class AnsweredQuestionBuilder extends Builder<AnsweredQuestion> {
  private noteToUse?: Note
  private recallPromptToUse?: RecallPrompt
  private isCorrect: boolean = true
  private choiceIndexToUse?: number

  withNote(note: Note): this {
    this.noteToUse = note
    return this
  }

  withRecallPromptId(id: number): this {
    this.recallPromptToUse = new RecallPromptBuilder().withId(id).do()
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
    const predefinedQuestion = makeMe.aPredefinedQuestion.please()
    return {
      answer: {
        id: generateId(),
        correct: this.isCorrect,
        ...(this.choiceIndexToUse !== undefined && {
          choiceIndex: this.choiceIndexToUse,
        }),
      },
      note: this.noteToUse ?? makeMe.aNote.please(),
      recallPrompt:
        this.recallPromptToUse ??
        new RecallPromptBuilder()
          .withPredefinedQuestion(predefinedQuestion)
          .do(),
    }
  }
}

export default AnsweredQuestionBuilder
