import Builder from "./Builder";
import NotePositionBuilder from "./NotePositionBuilder";
import generateId from "./generateId";

class QuizQuestionBuilder extends Builder<Generated.QuizQuestion> {
  notePositionBuilder: NotePositionBuilder = new NotePositionBuilder();

  quizQuestion: Generated.QuizQuestion = {
    quizQuestionId: generateId(),
    questionType: "SPELLING",
    choices: [],
    stem: "answer",
    mainTopic: "",
    headNotePosition: this.notePositionBuilder.do(),
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
    this.quizQuestion.headNotePosition = this.notePositionBuilder.do();
    return this.quizQuestion;
  }
}

export default QuizQuestionBuilder;
