import type { AssessmentQuestionInstance } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import PredefinedQuestionBuilder from "./PredefinedQuestionBuilder"

class AssessmentQuestionInstanceBuilder extends Builder<AssessmentQuestionInstance> {
  predefinedQuestionBuilder = new PredefinedQuestionBuilder()

  withQuestionStem(stem: string) {
    this.predefinedQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.predefinedQuestionBuilder.withChoices(choices)
    return this
  }

  do(): AssessmentQuestionInstance {
    const predefinedQuestion = this.predefinedQuestionBuilder.do()
    return {
      id: generateId(),
      multipleChoicesQuestion: predefinedQuestion.multipleChoicesQuestion,
    }
  }
}

export default AssessmentQuestionInstanceBuilder
