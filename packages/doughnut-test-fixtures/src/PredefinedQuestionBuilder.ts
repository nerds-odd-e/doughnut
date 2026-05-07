import type { PredefinedQuestion } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'

class PredefinedQuestionBuilder extends Builder<PredefinedQuestion> {
  predefinedQuestion: PredefinedQuestion = {
    id: generateId(),
    correctAnswerIndex: 0,
    multipleChoicesQuestion: {
      questionStem: 'answer',
      responseChoices: [],
    },
  }

  correctAnswerIndex(n: number) {
    this.predefinedQuestion.correctAnswerIndex = n
    return this
  }

  withQuestionStem(stem: string) {
    this.predefinedQuestion.multipleChoicesQuestion.questionStem = stem
    return this
  }

  withChoices(choices: string[]) {
    this.predefinedQuestion.multipleChoicesQuestion.responseChoices = [
      ...choices,
    ]
    return this
  }

  do(): PredefinedQuestion {
    return this.predefinedQuestion
  }
}

export default PredefinedQuestionBuilder
