package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import java.util.Map;
import java.util.Optional;

public interface QuizQuestionPresenter {
  String instruction();

  String mainTopic();

  default Map<Link.LinkType, LinkViewed> hintLinks() {
    return null;
  }

  default QuizQuestionViewedByUser.OptionCreator optionCreator() {
    return new QuizQuestionViewedByUser.TitleOptionCreator();
  }

  default Optional<PictureWithMask> pictureWithMask() {
    return Optional.empty();
  }
}
