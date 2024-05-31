package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import java.util.ArrayList;
import java.util.List;

public abstract class QuestionOptionsFactory extends QuizQuestionFactory {
  @Override
  public QuizQuestionEntity buildQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    QuizQuestionWithNoteChoices quizQuestion = this.buildQuizQuestionObj(servant);
    this.validateBasicPossibility();
    Note answerNote = this.generateAnswer(servant);
    if (answerNote == null) {
      throw new QuizQuestionNotPossibleException();
    }
    List<? extends Note> options = this.generateFillingOptions(servant);
    if (options.size() < this.minimumOptionCount() - 1) {
      throw new QuizQuestionNotPossibleException();
    }
    List<Note> optionsEntities = new ArrayList<>(options);
    optionsEntities.add(answerNote);
    List<Note> shuffled = servant.randomizer.shuffle(optionsEntities);
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
    mcqWithAnswer.stem = getStem();
    mcqWithAnswer.correctChoiceIndex = shuffled.indexOf(answerNote);
    mcqWithAnswer.choices =
        shuffled.stream().map(this::noteToChoice).map(QuizQuestion.Choice::getDisplay).toList();
    quizQuestion.setMcqWithAnswer(mcqWithAnswer);
    return quizQuestion;
  }

  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {}

  public abstract QuizQuestionWithNoteChoices buildQuizQuestionObj(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException;

  public abstract Note generateAnswer(QuizQuestionServant servant);

  public abstract List<? extends Note> generateFillingOptions(QuizQuestionServant servant);

  public int minimumOptionCount() {
    return 2;
  }

  public QuizQuestion.Choice noteToChoice(Note note) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    choice.setDisplay(note.getTopicConstructor());
    return choice;
  }
}
