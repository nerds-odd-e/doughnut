package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.entities.json.QuizQuestion;
import java.util.List;
import java.util.Optional;

public interface QuizQuestionPresenter {
  String instruction();

  String mainTopic();

  List<Note> knownRightAnswers();

  default LinksOfANote hintLinks() {
    return null;
  }

  default QuizQuestion.OptionCreator optionCreator() {
    return new QuizQuestion.TitleOptionCreator();
  }

  default Optional<PictureWithMask> pictureWithMask() {
    return Optional.empty();
  }
}
