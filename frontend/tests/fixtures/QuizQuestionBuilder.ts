import { QuizQuestion } from "@/generated/backend";
import Builder from "./Builder";
import generateId from "./generateId";
import NotebookBuilder from "./NotebookBuilder";

class QuizQuestionBuilder extends Builder<QuizQuestion> {
  quizQuestion: QuizQuestion = {
    id: generateId(),
    notebook: new NotebookBuilder().do(),
    multipleChoicesQuestion: {
      stem: "answer",
      choices: [],
    },
  };

  withQuestionStem(stem: string) {
    this.quizQuestion.multipleChoicesQuestion.stem = stem;
    return this;
  }

  withChoices(choices: string[]) {
    this.quizQuestion.multipleChoicesQuestion.choices = [...choices];
    return this;
  }

  do(): QuizQuestion {
    return this.quizQuestion;
  }
}

export default QuizQuestionBuilder;
