import type { PredefinedQuestion } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

class PredefinedQuestionBuilder extends Builder<PredefinedQuestion> {
  predefinedQuestion: PredefinedQuestion = {
    id: generateId(),
    correctAnswerIndex: 0,
    bareQuestion: {
      multipleChoicesQuestion: {
        stem: "answer",
        choices: [],
      },
    },
  }

  correctAnswerIndex(n: number) {
    this.predefinedQuestion.correctAnswerIndex = n
    return this
  }

  withSpellCheck() {
    this.predefinedQuestion.bareQuestion.checkSpell = true
    return this
  }

  withQuestionStem(stem: string) {
    this.predefinedQuestion.bareQuestion.multipleChoicesQuestion.stem = stem
    return this
  }

  withChoices(choices: string[]) {
    this.predefinedQuestion.bareQuestion.multipleChoicesQuestion.choices = [
      ...choices,
    ]
    return this
  }

  do(): PredefinedQuestion {
    return this.predefinedQuestion
  }
}

export default PredefinedQuestionBuilder
