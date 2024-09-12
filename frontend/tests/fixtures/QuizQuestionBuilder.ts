import { QuizQuestion } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import PredefinedQuestionBuilder from "./PredefinedQuestionBuilder"

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
      multipleChoicesQuestion: predefinedQuizQuestion.multipleChoicesQuestion,
      checkSpell: predefinedQuizQuestion.checkSpell,
    }
  }
}

export default QuizQuestionBuilder
