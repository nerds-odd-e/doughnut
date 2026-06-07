import type {
  PredefinedQuestion,
  Answer,
  RecallPromptHistoryItem,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'
import PredefinedQuestionBuilder from './PredefinedQuestionBuilder'

class RecallPromptBuilder extends Builder<RecallPromptHistoryItem> {
  predefinedQuestionBuilder = new PredefinedQuestionBuilder()
  private idToUse?: number
  private predefinedQuestionToUse?: PredefinedQuestion
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
    this.predefinedQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.predefinedQuestionBuilder.withChoices(choices)
    return this
  }

  withPredefinedQuestion(predefinedQuestion: PredefinedQuestion) {
    this.predefinedQuestionToUse = predefinedQuestion
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
    const predefinedQuestion =
      this.predefinedQuestionToUse ?? this.predefinedQuestionBuilder.do()
    return {
      id: this.idToUse ?? generateId(),
      questionType: (this.questionTypeToUse ?? 'MCQ') as 'MCQ' | 'SPELLING',
      multipleChoicesQuestion: predefinedQuestion.multipleChoicesQuestion,
      predefinedQuestion: predefinedQuestion,
      answer: this.answerToUse,
      answerTime: this.answerTimeToUse,
      questionGeneratedTime: this.questionGeneratedTimeToUse ?? '',
      isContested: this.isContestedToUse,
    }
  }
}

export default RecallPromptBuilder
