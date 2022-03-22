package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestion;

public class LinkTargetExclusiveQuizPresenter implements QuizQuestionPresenter {
    private final Link link;

    public LinkTargetExclusiveQuizPresenter(QuizQuestion quizQuestion) {
        this.link = quizQuestion.getReviewPoint().getLink();
    }

    @Override
    public String generateInstruction() {
        return String.format("Which of the following is <em>NOT</em> %s", link.getLinkType().label);
    }

}