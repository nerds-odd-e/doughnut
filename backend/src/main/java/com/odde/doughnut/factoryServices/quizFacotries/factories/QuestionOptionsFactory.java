package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.ArrayList;
import java.util.List;

public abstract class QuestionOptionsFactory extends QuizQuestionFactory {
  protected Note note;

  public QuestionOptionsFactory(Note note) {
    this.note = note;
  }

  @Override
  public QuizQuestion buildValidQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    QuizQuestion quizQuestion = new QuizQuestion();
    quizQuestion.setNote(note);
    quizQuestion.setHasImage(this instanceof ImageTitleSelectionQuizFactory);
    this.findCategoricalLink(servant);
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
    quizQuestion.setCorrectAnswerIndex(shuffled.indexOf(answerNote));
    MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
    mcq.setStem(getStem());
    mcq.setChoices(shuffled.stream().map(this::noteToChoice).toList());
    quizQuestion.setMultipleChoicesQuestion(mcq);
    return quizQuestion;
  }

  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {}

  public void findCategoricalLink(QuizQuestionServant servant) {}

  public abstract Note generateAnswer(QuizQuestionServant servant);

  public abstract List<? extends Note> generateFillingOptions(QuizQuestionServant servant);

  public int minimumOptionCount() {
    return 2;
  }

  public String noteToChoice(Note note) {
    return note.getTopicConstructor();
  }

  protected abstract String getStem();
}
