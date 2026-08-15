import type {
  Notebook,
  RecallPrompt,
  SpellingQuestion,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'
import NotebookBuilder from './NotebookBuilder'
import McqBuilder from './McqBuilder'

class RecallPromptBuilder extends Builder<RecallPrompt> {
  private idToUse?: number
  private notebookToUse?: Notebook
  private mcqBuilder = new McqBuilder()
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

  withNotebook(notebook: Notebook) {
    this.notebookToUse = notebook
    return this
  }

  withSpellingStem(stem: string) {
    this.spellingStemToUse = stem
    return this
  }

  do(): RecallPrompt {
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
    const mcq = this.mcqBuilder.do()
    return {
      id: this.idToUse ?? generateId(),
      notebook,
      mcq: {
        id: mcq.id,
        questionStem: mcq.questionStem,
        responseChoices: mcq.responseChoices,
      },
    }
  }
}

export default RecallPromptBuilder
