import type { SuggestedQuestionForFineTuning } from "@generated/backend"
import Builder from "./Builder"

class SuggestQuestionForFineTuningBuilder extends Builder<SuggestedQuestionForFineTuning> {
  data: SuggestedQuestionForFineTuning

  constructor() {
    super()
    this.data = {
      id: 1357,
      preservedQuestion: {
        f0__multipleChoicesQuestion: {
          f0__stem: "What is the capital of France?",
          f1__choices: ["Paris", "London", "Berlin"],
        },
        f1__correctChoiceIndex: 1,
        f2__strictChoiceOrder: false,
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
