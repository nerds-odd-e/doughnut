package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QuizQuestionPresenter {
  String instruction();

  String mainTopic();

  default Map<Link.LinkType, LinkViewed> hintLinks(User user) {
    return null;
  }

  default Optional<PictureWithMask> pictureWithMask() {
    return Optional.empty();
  }

  default List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService) {
    return List.of();
  }
}
