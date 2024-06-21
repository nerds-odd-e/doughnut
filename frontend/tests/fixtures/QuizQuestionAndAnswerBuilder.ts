import { QuizQuestionAndAnswer } from "@/generated/backend"
import Builder from "./Builder"
import QuizQuestionBuilder from "./QuizQuestionBuilder"

class QuizQuestionAndAnswerBuilder extends Builder<QuizQuestionAndAnswer> {
  quizQuestionBuilder = new QuizQuestionBuilder()

  withQuestionStem(stem: string) {
    this.quizQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.quizQuestionBuilder.withChoices(choices)
    return this
  }

  do(): QuizQuestionAndAnswer {
    const quizQuestion = this.quizQuestionBuilder.do()
    return {
      id: quizQuestion.id,
      quizQuestion,
    }
  }
}

export default QuizQuestionAndAnswerBuilder
