import Builder from "./Builder";

class SuggestQuestionForFineTuningBuilder extends Builder<Generated.SuggestedQuestionForFineTuning> {
  data: Generated.SuggestedQuestionForFineTuning;

  constructor() {
    super();
    this.data = {
      id: 1357,
      preservedQuestion: {
        stem: "What is the capital of France?",
        choices: ["Paris", "London", "Berlin", "Madrid"],
        correctChoiceIndex: 0,
        confidence: 9,
      },
      preservedNoteContent: "this is the note content",
      comment: "",
      positiveFeedback: false,
      duplicated: false,
    };
  }

  positive() {
    this.data.positiveFeedback = true;
    return this;
  }

  do() {
    return this.data;
  }
}

export default SuggestQuestionForFineTuningBuilder;
