import { ReviewQuestionInstance } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import PredefinedQuestionBuilder from "./PredefinedQuestionBuilder"
import NotebookBuilder from "./NotebookBuilder"

class QuizQuestionBuilder extends Builder<ReviewQuestionInstance> {
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
    const predefinedQuizQuestion = this.predefinedQuestionBuilder.do()
    return {
      id: generateId(),
      quizQuestion1: predefinedQuizQuestion.quizQuestion1,
      notebook: new NotebookBuilder().do(),
    }
  }
}

export default QuizQuestionBuilder
