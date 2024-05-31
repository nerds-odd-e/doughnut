package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class ImageSelectionQuizFactory extends QuestionOptionsFactory {
  private final Note answerNote;

  public ImageSelectionQuizFactory(Note note) {
    this.answerNote = note;
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    return servant.chooseFromCohort(answerNote, n -> n.getImageWithMask() != null);
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
    return answerNote;
  }

  @Override
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    if (answerNote.getImageWithMask() == null) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {

    QuizQuestionEntity quizQuestion = new QuizQuestionEntity();
    quizQuestion.setNote(answerNote);
    return quizQuestion;
  }

  @Override
  public QuizQuestion.Choice noteToChoice(Note note) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    choice.setDisplay(note.getTopicConstructor());
    choice.setImageWithMask(note.getImageWithMask());
    choice.setImage(true);
    return choice;
  }

  @Override
  public String getStem() {
    return "<strong>" + answerNote.getTopicConstructor() + "</strong>";
  }
}
