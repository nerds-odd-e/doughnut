package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;

public abstract class ClozeDescriptonQuizPresenter implements QuizQuestionPresenter {
    private QuizQuestion quizQuestion;

    public ClozeDescriptonQuizPresenter(QuizQuestion quizQuestion) {
        this.quizQuestion = quizQuestion;
    }

    @Override
    public String generateInstruction() {
        return quizQuestion.getReviewPoint().getNote().getClozeDescription();
    }

}