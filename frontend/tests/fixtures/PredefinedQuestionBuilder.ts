import { PredefinedQuestion } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

class PredefinedQuestionBuilder extends Builder<PredefinedQuestion> {
  predefinedQuestion: PredefinedQuestion = {
    id: generateId(),
    multipleChoicesQuestion: {
      stem: "answer",
      choices: [],
    },
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
