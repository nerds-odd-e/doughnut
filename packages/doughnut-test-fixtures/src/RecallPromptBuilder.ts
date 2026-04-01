import type {
  AssessmentQuestionInstance,
  Notebook,
  RecallPrompt,
  Note,
  PredefinedQuestion,
  Answer,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'
import PredefinedQuestionBuilder from './PredefinedQuestionBuilder'
import NotebookBuilder from './NotebookBuilder'

class RecallPromptBuilder extends Builder<RecallPrompt> {
  predefinedQuestionBuilder = new PredefinedQuestionBuilder()
  private idToUse?: number
  private notebookToUse?: Notebook
  private noteToUse?: Note
  private predefinedQuestionToUse?: PredefinedQuestion
  private answerToUse?: Answer
  private answerTimeToUse?: string
  private questionGeneratedTimeToUse?: string
  private isContestedToUse?: boolean
  private questionTypeToUse?: string
  private memoryTrackerIdToUse?: number
  private spellingStemToUse?: string
  private assessmentInstanceToUse?: AssessmentQuestionInstance

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

  withNotebook(notebook: Notebook) {
    this.notebookToUse = notebook
    return this
  }

  withNote(note: Note) {
    this.noteToUse = note
    return this
  }

  forAssessmentQuestion(
    instance: AssessmentQuestionInstance,
    notebook?: Notebook
  ) {
    this.assessmentInstanceToUse = instance
    if (notebook !== undefined) {
      this.notebookToUse = notebook
    }
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

  withMemoryTrackerId(id: number) {
    this.memoryTrackerIdToUse = id
    return this
  }

  withSpellingStem(stem: string) {
    this.spellingStemToUse = stem
    this.questionTypeToUse = 'SPELLING'
    return this
  }

  do(): RecallPrompt {
    const notebook = this.notebookToUse ?? new NotebookBuilder().do()
    if (this.spellingStemToUse !== undefined) {
      return {
        id: this.idToUse ?? generateId(),
        memoryTrackerId: this.memoryTrackerIdToUse,
        questionType: 'SPELLING',
        notebook,
        note: this.noteToUse,
        spellingQuestion: { stem: this.spellingStemToUse },
        answer: this.answerToUse,
        answerTime: this.answerTimeToUse,
        questionGeneratedTime: this.questionGeneratedTimeToUse,
        isContested: this.isContestedToUse,
      }
    }
    if (this.assessmentInstanceToUse !== undefined) {
      const inst = this.assessmentInstanceToUse
      return {
        id: this.idToUse ?? inst.id,
        memoryTrackerId: this.memoryTrackerIdToUse,
        questionType: 'MCQ',
        multipleChoicesQuestion: inst.multipleChoicesQuestion,
        notebook,
        note: this.noteToUse,
        answer: this.answerToUse ?? inst.answer,
        answerTime: this.answerTimeToUse,
        questionGeneratedTime: this.questionGeneratedTimeToUse,
        isContested: this.isContestedToUse,
      }
    }
    const predefinedQuestion =
      this.predefinedQuestionToUse ?? this.predefinedQuestionBuilder.do()
    return {
      id: this.idToUse ?? generateId(),
      memoryTrackerId: this.memoryTrackerIdToUse,
      questionType: (this.questionTypeToUse ?? 'MCQ') as 'MCQ' | 'SPELLING',
      multipleChoicesQuestion: predefinedQuestion.multipleChoicesQuestion,
      notebook,
      note: this.noteToUse,
      predefinedQuestion: predefinedQuestion,
      answer: this.answerToUse,
      answerTime: this.answerTimeToUse,
      questionGeneratedTime: this.questionGeneratedTimeToUse,
      isContested: this.isContestedToUse,
    }
  }
}

export default RecallPromptBuilder
