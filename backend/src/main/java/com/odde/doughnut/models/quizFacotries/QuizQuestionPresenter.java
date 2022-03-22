package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.List;
import java.util.Map;

public interface QuizQuestionPresenter {
    String instruction();
    String mainTopic();

    default Map<Link.LinkType, LinkViewed> hintLinks() {
        return null;
    }

    default QuizQuestion.OptionCreator optionCreator() {
        return new QuizQuestion.TitleOptionCreator();
    }

}
