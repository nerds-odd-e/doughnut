package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.ArrayList;
import java.util.List;

public abstract class QuestionOptionsFactory extends PredefinedQuestionFactory {
  protected final Note note;
  protected final PredefinedQuestionServant servant;

  public QuestionOptionsFactory(Note note, PredefinedQuestionServant servant) {
    this.note = note;
    this.servant = servant;
  }

  @Override
  public PredefinedQuestion buildValidPredefinedQuestion()
      throws PredefinedQuestionNotPossibleException {
    PredefinedQuestion predefinedQuestion = new PredefinedQuestion();
    predefinedQuestion.setNote(note);
    this.findCategoricalLink();
    this.validateBasicPossibility();
    Note answerNote = this.generateAnswer();
    if (answerNote == null) {
      throw new PredefinedQuestionNotPossibleException();
    }
    List<Note> options = this.generateFillingOptions();
    if (options.size() < this.minimumOptionCount() - 1) {
      throw new PredefinedQuestionNotPossibleException();
    }
    List<Note> optionsEntities = new ArrayList<>(options);
    optionsEntities.add(answerNote);
    List<Note> shuffled = servant.randomizer.shuffle(optionsEntities);
    predefinedQuestion.setCorrectAnswerIndex(shuffled.indexOf(answerNote));
    MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
    mcq.setStem(getStem());
    mcq.setChoices(shuffled.stream().map(this::noteToChoice).toList());
    predefinedQuestion.getBareQuestion().setMultipleChoicesQuestion(mcq);
    return predefinedQuestion;
  }

  public void validateBasicPossibility() throws PredefinedQuestionNotPossibleException {}

  public void findCategoricalLink() {}

  public abstract Note generateAnswer();

  public abstract List<Note> generateFillingOptions();

  public int minimumOptionCount() {
    return 2;
  }

  public String noteToChoice(Note note) {
    return note.getTopicConstructor();
  }

  protected abstract String getStem();
}
