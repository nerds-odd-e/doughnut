import type { PredefinedQuestion } from "generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

class PredefinedQuestionBuilder extends Builder<PredefinedQuestion> {
  predefinedQuestion: PredefinedQuestion = {
    id: generateId(),
    correctAnswerIndex: 0,
    multipleChoicesQuestion: {
      stem: "answer",
      choices: [],
    },
  }

  correctAnswerIndex(n: number) {
    this.predefinedQuestion.correctAnswerIndex = n
    return this
  }

  withQuestionStem(stem: string) {
    this.predefinedQuestion.multipleChoicesQuestion.stem = stem
    return this
  }

  withChoices(choices: string[]) {
    this.predefinedQuestion.multipleChoicesQuestion.choices = [...choices]
    return this
  }

  do(): PredefinedQuestion {
    return this.predefinedQuestion
  }
}

export default PredefinedQuestionBuilder
