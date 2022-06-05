package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import java.util.List;
import java.util.Optional;

public interface QuizQuestionPresenter {
  String instruction();

  String mainTopic();

  List<Note> knownRightAnswers();

  default LinksOfANote hintLinks() {
    return null;
  }

  default QuizQuestionViewedByUser.OptionCreator optionCreator() {
    return new QuizQuestionViewedByUser.TitleOptionCreator();
  }

  default Optional<PictureWithMask> pictureWithMask() {
    return Optional.empty();
  }
}
