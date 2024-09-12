import { QuizQuestion } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import PredefinedQuestionBuilder from "./PredefinedQuestionBuilder"
import NotebookBuilder from "./NotebookBuilder"

class QuizQuestionBuilder extends Builder<QuizQuestion> {
  predefinedQuestionBuilder = new PredefinedQuestionBuilder()

  withQuestionStem(stem: string) {
    this.predefinedQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.predefinedQuestionBuilder.withChoices(choices)
    return this
  }

  do(): QuizQuestion {
    const predefinedQuizQuestion = this.predefinedQuestionBuilder.do()
    return {
      id: generateId(),
      ...predefinedQuizQuestion.quizQuestion1,
      notebook: new NotebookBuilder().do(),
    }
  }
}

export default QuizQuestionBuilder
