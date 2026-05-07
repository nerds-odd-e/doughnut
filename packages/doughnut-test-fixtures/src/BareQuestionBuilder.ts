import type { MultipleChoicesQuestion } from '@generated/doughnut-backend-api'
import Builder from './Builder'

class MultipleChoicesQuestionBuilder extends Builder<MultipleChoicesQuestion> {
  private multipleChoicesQuestion: MultipleChoicesQuestion = {
    questionStem: '',
    responseChoices: [],
  }

  withStem(stem: string): this {
    this.multipleChoicesQuestion.questionStem = stem
    return this
  }

  withChoices(choices: string[]): this {
    this.multipleChoicesQuestion.responseChoices = choices
    return this
  }

  do(): MultipleChoicesQuestion {
    return this.multipleChoicesQuestion
  }
}

export default MultipleChoicesQuestionBuilder
