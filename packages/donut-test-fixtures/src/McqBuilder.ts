import type { Mcq } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'

class McqBuilder extends Builder<Mcq> {
  mcq: Mcq = {
    id: generateId(),
    correctAnswerIndex: 0,
    questionStem: 'answer',
    responseChoices: [],
  }

  correctAnswerIndex(n: number) {
    this.mcq.correctAnswerIndex = n
    return this
  }

  withQuestionStem(stem: string) {
    this.mcq.questionStem = stem
    return this
  }

  withChoices(choices: string[]) {
    this.mcq.responseChoices = [...choices]
    return this
  }

  testedFocus(focus: string) {
    this.mcq.testedFocus = focus
    return this
  }

  do(): Mcq {
    return this.mcq
  }
}

export default McqBuilder
