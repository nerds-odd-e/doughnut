import { QuizQuestion } from "@/generated/backend";
import Builder from "./Builder";
import NoteBuilder from "./NoteBuilder";
import generateId from "./generateId";

class QuizQuestionBuilder extends Builder<QuizQuestion> {
  quizQuestion: QuizQuestion = {
    id: generateId(),
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

  do(): QuizQuestion {
    return this.quizQuestion;
  }
}

export default QuizQuestionBuilder;
