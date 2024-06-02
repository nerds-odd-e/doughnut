import { QuizQuestion } from "@/generated/backend";
import Builder from "./Builder";
import NoteBuilder from "./NoteBuilder";
import generateId from "./generateId";

class QuizQuestionBuilder extends Builder<QuizQuestion> {
  quizQuestion: QuizQuestion = {
    id: generateId(),
    choices: [],
    stem: "answer",
    headNote: new NoteBuilder().do(),
    multipleChoicesQuestion: {
      stem: "answer",
      choices: [],
    },
  };

  withQuestionStem(stem: string) {
    this.quizQuestion.stem = stem;
    this.quizQuestion.multipleChoicesQuestion.stem = stem;
    return this;
  }

  withChoices(choices: string[]) {
    this.quizQuestion.choices = choices.map((choice) => ({
      image: false,
      display: choice,
    }));
    this.quizQuestion.multipleChoicesQuestion.choices = choices;
    return this;
  }

  do(): QuizQuestion {
    return this.quizQuestion;
  }
}

export default QuizQuestionBuilder;
