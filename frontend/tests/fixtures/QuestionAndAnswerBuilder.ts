import { QuestionAndAnswer } from "@/generated/backend"
import Builder from "./Builder"
import QuizQuestionBuilder from "./QuizQuestionBuilder"

class QuestionAndAnswerBuilder extends Builder<QuestionAndAnswer> {
  quizQuestionBuilder = new QuizQuestionBuilder()

  withQuestionStem(stem: string) {
    this.quizQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.quizQuestionBuilder.withChoices(choices)
    return this
  }

  do(): QuestionAndAnswer {
    const quizQuestion = this.quizQuestionBuilder.do()
    return {
      id: quizQuestion.id,
      multipleChoicesQuestion: quizQuestion.multipleChoicesQuestion,
      quizQuestion,
    }
  }
}

export default QuestionAndAnswerBuilder
