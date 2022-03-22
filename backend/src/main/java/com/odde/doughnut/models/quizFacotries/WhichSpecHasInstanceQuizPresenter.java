package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestion;

public class WhichSpecHasInstanceQuizPresenter implements QuizQuestionPresenter {
    private Link instanceLink;
    private final Link link;

    public WhichSpecHasInstanceQuizPresenter(QuizQuestion quizQuestion) {
        this.link = quizQuestion.getReviewPoint().getLink();
        this.instanceLink = quizQuestion.getViceReviewPoints().get(0).getLink();
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one is " + link.getLinkTypeLabel() + " <mark>"+link.getTargetNote().getTitle()+"</mark> <em>and</em> is " + instanceLink.getLinkTypeLabel() + " <mark>" + instanceLink.getTargetNote().getTitle() + "</mark>:" ;
    }

}