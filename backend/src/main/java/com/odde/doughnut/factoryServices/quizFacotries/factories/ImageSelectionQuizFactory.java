package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class ImageSelectionQuizFactory extends QuestionOptionsFactory {

  public ImageSelectionQuizFactory(Note note) {
    super(note);
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    return servant.chooseFromCohort(note, n -> n.getImageWithMask() != null);
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
    return note;
  }

  @Override
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    if (note.getImageWithMask() == null) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestion.Choice noteToChoice(Note n) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    choice.setDisplay(n.getTopicConstructor());
    choice.setImageWithMask(n.getImageWithMask());
    choice.setImage(true);
    return choice;
  }

  @Override
  public String getStem() {
    return "<strong>" + note.getTopicConstructor() + "</strong>";
  }
}
