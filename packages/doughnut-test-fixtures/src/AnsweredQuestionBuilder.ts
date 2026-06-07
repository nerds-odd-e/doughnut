import type {
  AnsweredQuestion,
  Answer,
  Note,
  PredefinedQuestion,
  RecallPromptHistoryItem,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'
import makeMe from './makeMe'
class AnsweredQuestionBuilder extends Builder<AnsweredQuestion> {
  private noteToUse?: Note
  private isCorrect = true
  private choiceIndexToUse?: number
  private propertyKeyToUse?: string
  private memoryTrackerIdToUse = generateId()
  private idToUse?: number
  private questionType: 'MCQ' | 'SPELLING' = 'MCQ'
  private predefinedQuestionToUse?: PredefinedQuestion
  private answerToUse?: Answer
  private notebookIdToUse = generateId()

  withNote(note: Note): this {
    this.noteToUse = note
    return this
  }

  withId(id: number): this {
    this.idToUse = id
    return this
  }

  answerCorrect(correct: boolean): this {
    this.isCorrect = correct
    return this
  }

  withChoiceIndex(index: number): this {
    this.choiceIndexToUse = index
    return this
  }

  withPropertyKey(propertyKey: string): this {
    this.propertyKeyToUse = propertyKey
    return this
  }

  withMemoryTrackerId(id: number): this {
    this.memoryTrackerIdToUse = id
    return this
  }

  withPredefinedQuestion(predefinedQuestion: PredefinedQuestion): this {
    this.predefinedQuestionToUse = predefinedQuestion
    return this
  }

  withAnswer(answer: Answer): this {
    this.answerToUse = answer
    return this
  }

  fromMcqHistoryItem(
    pending: RecallPromptHistoryItem,
    note: Note,
    memoryTrackerId: number
  ): this {
    this.idToUse = pending.id
    this.noteToUse = note
    this.predefinedQuestionToUse = pending.predefinedQuestion
    this.memoryTrackerIdToUse = memoryTrackerId
    return this
  }

  spelling(): this {
    this.questionType = 'SPELLING'
    return this
  }

  do(): AnsweredQuestion {
    const note = this.noteToUse ?? makeMe.aNote.please()
    const answer =
      this.answerToUse ??
      ({
        id: generateId(),
        correct: this.isCorrect,
        ...(this.choiceIndexToUse !== undefined && {
          choiceIndex: this.choiceIndexToUse,
        }),
      } as Answer)
    const predefinedQuestion =
      this.questionType === 'MCQ'
        ? (this.predefinedQuestionToUse ?? makeMe.aPredefinedQuestion.please())
        : undefined
    return {
      id: this.idToUse ?? generateId(),
      questionType: this.questionType,
      memoryTrackerId: this.memoryTrackerIdToUse,
      recalledNote: {
        noteTopology: note.noteTopology,
        notebookId: this.notebookIdToUse,
        ancestorFolders: [],
        propertyKey: this.propertyKeyToUse,
      },
      answer,
      predefinedQuestion,
    }
  }
}

export default AnsweredQuestionBuilder
