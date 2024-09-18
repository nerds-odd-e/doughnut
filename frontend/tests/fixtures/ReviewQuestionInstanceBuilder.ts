import type { ReviewQuestionInstance } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import PredefinedQuestionBuilder from "./PredefinedQuestionBuilder"
import NotebookBuilder from "./NotebookBuilder"

class ReviewQuestionInstanceBuilder extends Builder<ReviewQuestionInstance> {
  predefinedQuestionBuilder = new PredefinedQuestionBuilder()

  withQuestionStem(stem: string) {
    this.predefinedQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.predefinedQuestionBuilder.withChoices(choices)
    return this
  }

  do(): ReviewQuestionInstance {
    const predefinedQuestion = this.predefinedQuestionBuilder.do()
    return {
      id: generateId(),
      bareQuestion: predefinedQuestion.bareQuestion,
      notebook: new NotebookBuilder().do(),
    }
  }
}

export default ReviewQuestionInstanceBuilder
