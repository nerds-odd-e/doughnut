import Builder from "./Builder";
import NoteBuilder from "./NoteBuilder";
import generateId from "./generateId";

class QuizQuestionBuilder extends Builder<Generated.QuizQuestion> {
  quizQuestion: Generated.QuizQuestion = {
    quizQuestionId: generateId(),
    choices: [],
    stem: "answer",
    mainTopic: "",
    headNote: new NoteBuilder().do(),
  };

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
