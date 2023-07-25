import Builder from "./Builder";
import generateId from "./generateId";

class QuizQuestionBuilder extends Builder<Generated.QuizQuestion> {
  quizQuestion: Generated.QuizQuestion = {
    quizQuestionId: generateId(),
    questionType: "JUST_REVIEW",
    choices: [
      {
        picture: false,
        display: "question",
      },
    ],
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

  withChoices(choices: string[]) {
    this.quizQuestion.choices = choices.map((choice) => ({
      picture: false,
      display: choice,
    }));
    return this;
  }

  do(): Generated.QuizQuestion {
    return this.quizQuestion;
  }
}

export default QuizQuestionBuilder;
