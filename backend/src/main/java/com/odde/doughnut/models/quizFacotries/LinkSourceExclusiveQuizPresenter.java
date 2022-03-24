package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestion;

public class LinkSourceExclusiveQuizPresenter implements QuizQuestionPresenter {
    private final Link link;

    public LinkSourceExclusiveQuizPresenter(QuizQuestion quizQuestion) {
        this.link = quizQuestion.getReviewPoint().getLink();
    }

    @Override
    public String mainTopic() {
        return link.getTargetNote().getTitle();
    }

    @Override
    public String instruction() {
        return String.format("Which of the following is <em>NOT</em> %s", link.getLinkType().label);
    }

}