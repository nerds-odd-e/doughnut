import type {
  Mcq,
  Answer,
  RecallPromptHistoryItem,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'
import McqBuilder from './McqBuilder'

class RecallPromptHistoryItemBuilder extends Builder<RecallPromptHistoryItem> {
  mcqBuilder = new McqBuilder()
  private idToUse?: number
  private mcqToUse?: Mcq
  private answerToUse?: Answer
  private answerTimeToUse?: string
  private questionGeneratedTimeToUse?: string
  private isContestedToUse?: boolean
  private questionTypeToUse?: string
  private spellingStemToUse?: string

  withId(id: number) {
    this.idToUse = id
    return this
  }

  withQuestionStem(stem: string) {
    this.mcqBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.mcqBuilder.withChoices(choices)
    return this
  }

  withMcq(mcq: Mcq) {
    this.mcqToUse = mcq
    return this
  }

  withAnswer(answer: Answer) {
    this.answerToUse = answer
    return this
  }

  withAnswerTime(answerTime: string) {
    this.answerTimeToUse = answerTime
    return this
  }

  withQuestionGeneratedTime(questionGeneratedTime: string) {
    this.questionGeneratedTimeToUse = questionGeneratedTime
    return this
  }

  withIsContested(isContested: boolean) {
    this.isContestedToUse = isContested
    return this
  }

  withQuestionType(questionType: string) {
    this.questionTypeToUse = questionType
    return this
  }

  withSpellingStem(_stem: string) {
    this.spellingStemToUse = _stem
    this.questionTypeToUse = 'SPELLING'
    return this
  }

  spelling() {
    this.questionTypeToUse = 'SPELLING'
    return this
  }

  do(): RecallPromptHistoryItem {
    if (
      this.spellingStemToUse !== undefined ||
      this.questionTypeToUse === 'SPELLING'
    ) {
      return {
        id: this.idToUse ?? generateId(),
        questionType: 'SPELLING',
        questionGeneratedTime: this.questionGeneratedTimeToUse ?? '',
        answer: this.answerToUse,
        answerTime: this.answerTimeToUse,
        isContested: this.isContestedToUse,
      }
    }
    const mcq = this.mcqToUse ?? this.mcqBuilder.do()
    return {
      id: this.idToUse ?? generateId(),
      questionType: (this.questionTypeToUse ?? 'MCQ') as 'MCQ' | 'SPELLING',
      mcq,
      answer: this.answerToUse,
      answerTime: this.answerTimeToUse,
      questionGeneratedTime: this.questionGeneratedTimeToUse ?? '',
      isContested: this.isContestedToUse,
    }
  }
}

export default RecallPromptHistoryItemBuilder
