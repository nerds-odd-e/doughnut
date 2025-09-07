import type { RecallPrompt } from "@generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"
import PredefinedQuestionBuilder from "./PredefinedQuestionBuilder"
import NotebookBuilder from "./NotebookBuilder"

class RecallPromptBuilder extends Builder<RecallPrompt> {
  predefinedQuestionBuilder = new PredefinedQuestionBuilder()

  withQuestionStem(stem: string) {
    this.predefinedQuestionBuilder.withQuestionStem(stem)
    return this
  }

  withChoices(choices: string[]) {
    this.predefinedQuestionBuilder.withChoices(choices)
    return this
  }

  do(): RecallPrompt {
    const predefinedQuestion = this.predefinedQuestionBuilder.do()
    return {
      id: generateId(),
      multipleChoicesQuestion: predefinedQuestion.multipleChoicesQuestion,
      notebook: new NotebookBuilder().do(),
    }
  }
}

export default RecallPromptBuilder
