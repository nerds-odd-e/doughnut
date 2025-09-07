import type { SuggestedQuestionForFineTuning } from "@generated/backend"
import Builder from "./Builder"

class SuggestQuestionForFineTuningBuilder extends Builder<SuggestedQuestionForFineTuning> {
  data: SuggestedQuestionForFineTuning

  constructor() {
    super()
    this.data = {
      id: 1357,
      preservedQuestion: {
        multipleChoicesQuestion: {
          stem: "What is the capital of France?",
          choices: ["Paris", "London", "Berlin"],
        },
        correctChoiceIndex: 1,
        strictChoiceOrder: false,
      },
      preservedNoteContent: "this is the note content",
      comment: "",
      positiveFeedback: false,
      realCorrectAnswers: "0",
    }
  }

  positive() {
    this.data.positiveFeedback = true
    return this
  }

  do() {
    return this.data
  }
}

export default SuggestQuestionForFineTuningBuilder
