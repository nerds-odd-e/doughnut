package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.models.UserModel;

import java.util.List;
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
