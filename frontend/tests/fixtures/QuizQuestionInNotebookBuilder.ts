import { QuizQuestionInNotebook } from "@/generated/backend"
import Builder from "./Builder"
import NotebookBuilder from "./NotebookBuilder"
import QuizQuestionBuilder from "./QuizQuestionBuilder"

class QuizQuestionInNotebookBuilder extends Builder<QuizQuestionInNotebook> {
  quizQuestionBuilder = new QuizQuestionBuilder()

  withQuestionStem(stem: string) {
    this.quizQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.quizQuestionBuilder.withChoices(choices)
    return this
  }

  do(): QuizQuestionInNotebook {
    return {
      notebook: new NotebookBuilder().do(),
      quizQuestion: this.quizQuestionBuilder.do(),
    }
  }
}

export default QuizQuestionInNotebookBuilder
