import type {
  RecallPrompt,
  Note,
  PredefinedQuestion,
  Answer,
} from "@generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import PredefinedQuestionBuilder from "./PredefinedQuestionBuilder"
import NotebookBuilder from "./NotebookBuilder"

class RecallPromptBuilder extends Builder<RecallPrompt> {
  predefinedQuestionBuilder = new PredefinedQuestionBuilder()
  private noteToUse?: Note
  private predefinedQuestionToUse?: PredefinedQuestion
  private answerToUse?: Answer
  private answerTimeToUse?: string

  withQuestionStem(stem: string) {
    this.predefinedQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.predefinedQuestionBuilder.withChoices(choices)
    return this
  }

  withNote(note: Note) {
    this.noteToUse = note
    return this
  }

  withPredefinedQuestion(predefinedQuestion: PredefinedQuestion) {
    this.predefinedQuestionToUse = predefinedQuestion
    return this
  }

  withAnswer(answer: Answer) {
    this.answerToUse = answer
    return this
  }

  withAnswerTime(answerTime: string) {
    this.answerTimeToUse = answerTime
    return this
  }

  do(): RecallPrompt {
    const predefinedQuestion =
      this.predefinedQuestionToUse ?? this.predefinedQuestionBuilder.do()
    return {
      id: generateId(),
      multipleChoicesQuestion: predefinedQuestion.multipleChoicesQuestion,
      notebook: new NotebookBuilder().do(),
      note: this.noteToUse,
      predefinedQuestion: predefinedQuestion,
      answer: this.answerToUse,
      answerTime: this.answerTimeToUse,
    }
  }
}

export default RecallPromptBuilder
