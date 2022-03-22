package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestion;

public class LinkTargetQuizPresenter implements QuizQuestionPresenter {
    protected final Link link;

    public LinkTargetQuizPresenter(QuizQuestion quizQuestion) {
        this.link = quizQuestion.getReviewPoint().getLink();
    }

    @Override
    public String generateInstruction() {
        return "<mark>" + link.getSourceNote().getTitle() + "</mark> is " + link.getLinkTypeLabel() + ":";
    }
}