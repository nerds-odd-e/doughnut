import type { Note, RecallPrompt } from "@generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import makeMe from "./makeMe"
import RecallPromptBuilder from "./RecallPromptBuilder"

class AnsweredQuestionBuilder extends Builder<RecallPrompt> {
  private noteToUse?: Note
  private isCorrect: boolean = true
  private choiceIndexToUse?: number

  withNote(note: Note): this {
    this.noteToUse = note
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

  do(): RecallPrompt {
    const predefinedQuestion = makeMe.aPredefinedQuestion.please()
    const note = this.noteToUse ?? makeMe.aNote.please()
    return new RecallPromptBuilder()
      .withPredefinedQuestion(predefinedQuestion)
      .withNote(note)
      .withAnswer({
        id: generateId(),
        correct: this.isCorrect,
        ...(this.choiceIndexToUse !== undefined && {
          choiceIndex: this.choiceIndexToUse,
        }),
      })
      .do()
  }
}

export default AnsweredQuestionBuilder
