import type { MultipleChoicesQuestion } from "@/generated/backend"
import Builder from "./Builder"

class MultipleChoicesQuestionBuilder extends Builder<MultipleChoicesQuestion> {
  private multipleChoicesQuestion: MultipleChoicesQuestion = {
    stem: "",
    choices: [],
  }

  withStem(stem: string): this {
    this.multipleChoicesQuestion.stem = stem
    return this
  }

  withChoices(choices: string[]): this {
    this.multipleChoicesQuestion.choices = choices
    return this
  }

  do(): MultipleChoicesQuestion {
    return this.multipleChoicesQuestion
  }
}

export default MultipleChoicesQuestionBuilder
