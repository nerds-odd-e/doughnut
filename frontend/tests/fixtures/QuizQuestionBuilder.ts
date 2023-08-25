import Builder from "./Builder";
import generateId from "./generateId";

class QuizQuestionBuilder extends Builder<Generated.QuizQuestion> {
  quizQuestion: Generated.QuizQuestion = {
    quizQuestionId: generateId(),
    questionType: "SPELLING",
    choices: [],
    stem: "answer",
    mainTopic: "",
  };

  withClozeSelectionQuestion() {
    return this.withQuestionType("CLOZE_SELECTION");
  }

  withQuestionType(questionType: Generated.QuestionType) {
    this.quizQuestion.questionType = questionType;
    return this;
  }

  withQuestionStem(stem: string) {
    this.quizQuestion.stem = stem;
    return this;
  }

  withChoices(choices: { display: string; reason: string }[]) {
    this.quizQuestion.choices = choices.map((choice) => ({
      picture: false,
      display: choice.display,
      reason: choice.reason,
    }));
    return this;
  }

  do(): Generated.QuizQuestion {
    return this.quizQuestion;
  }
}

export default QuizQuestionBuilder;
