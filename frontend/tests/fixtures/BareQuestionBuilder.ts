import type { BareQuestion } from "@/generated/backend"
import Builder from "./Builder"

class BareQuestionBuilder extends Builder<BareQuestion> {
  private bareQuestion: BareQuestion = {
    multipleChoicesQuestion: {
      stem: "",
      choices: [],
    },
  }

  withStem(stem: string): this {
    this.bareQuestion.multipleChoicesQuestion.stem = stem
    return this
  }

  withChoices(choices: string[]): this {
    this.bareQuestion.multipleChoicesQuestion.choices = choices
    return this
  }

  do(): BareQuestion {
    return this.bareQuestion
  }
}

export default BareQuestionBuilder
