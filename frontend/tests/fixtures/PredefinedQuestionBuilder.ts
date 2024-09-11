import { PredefinedQuestion } from "@/generated/backend"
import Builder from "./Builder"
import QuizQuestionBuilder from "./QuizQuestionBuilder"

class PredefinedQuestionBuilder extends Builder<PredefinedQuestion> {
  quizQuestionBuilder = new QuizQuestionBuilder()

  withQuestionStem(stem: string) {
    this.quizQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.quizQuestionBuilder.withChoices(choices)
    return this
  }

  do(): PredefinedQuestion {
    const quizQuestion = this.quizQuestionBuilder.do()
    return {
      id: quizQuestion.id,
      multipleChoicesQuestion: quizQuestion.multipleChoicesQuestion,
      quizQuestion,
    }
  }
}

export default PredefinedQuestionBuilder
