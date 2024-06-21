import { QuizQuestion } from "@/generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

class QuizQuestionBuilder extends Builder<QuizQuestion> {
  quizQuestion: QuizQuestion = {
    id: generateId(),
    multipleChoicesQuestion: {
      stem: "answer",
      choices: [],
    },
  }

  withQuestionStem(stem: string) {
    this.quizQuestion.multipleChoicesQuestion.stem = stem
    return this
  }

  withChoices(choices: string[]) {
    this.quizQuestion.multipleChoicesQuestion.choices = [...choices]
    return this
  }

  do(): QuizQuestion {
    return this.quizQuestion
  }
}

export default QuizQuestionBuilder
