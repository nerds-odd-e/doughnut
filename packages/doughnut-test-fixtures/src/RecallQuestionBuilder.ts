import type {
  Notebook,
  RecallQuestion,
  SpellingQuestion,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'
import NotebookBuilder from './NotebookBuilder'
import PredefinedQuestionBuilder from './PredefinedQuestionBuilder'

class RecallQuestionBuilder extends Builder<RecallQuestion> {
  private idToUse?: number
  private notebookToUse?: Notebook
  private predefinedQuestionBuilder = new PredefinedQuestionBuilder()
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

  withNotebook(notebook: Notebook) {
    this.notebookToUse = notebook
    return this
  }

  withSpellingStem(stem: string) {
    this.spellingStemToUse = stem
    return this
  }

  do(): RecallQuestion {
    const notebook = this.notebookToUse ?? new NotebookBuilder().do()
    if (this.spellingStemToUse !== undefined) {
      const spellingQuestion: SpellingQuestion = {
        stem: this.spellingStemToUse,
        notebook,
      }
      return {
        id: this.idToUse ?? generateId(),
        notebook,
        spellingQuestion,
      }
    }
    const predefinedQuestion = this.predefinedQuestionBuilder.do()
    return {
      id: this.idToUse ?? generateId(),
      notebook,
      multipleChoicesQuestion: predefinedQuestion.multipleChoicesQuestion,
    }
  }
}

export default RecallQuestionBuilder
