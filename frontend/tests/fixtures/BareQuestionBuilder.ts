import type { MultipleChoicesQuestion } from "@generated/backend"
import Builder from "./Builder"

class MultipleChoicesQuestionBuilder extends Builder<MultipleChoicesQuestion> {
  private multipleChoicesQuestion: MultipleChoicesQuestion = {
    f0__stem: "",
    f1__choices: [],
  }

  withStem(stem: string): this {
    this.multipleChoicesQuestion.f0__stem = stem
    return this
  }

  withChoices(choices: string[]): this {
    this.multipleChoicesQuestion.f1__choices = choices
    return this
  }

  do(): MultipleChoicesQuestion {
    return this.multipleChoicesQuestion
  }
}

export default MultipleChoicesQuestionBuilder
