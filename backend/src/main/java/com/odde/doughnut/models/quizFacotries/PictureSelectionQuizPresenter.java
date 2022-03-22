package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;

public class PictureSelectionQuizPresenter implements QuizQuestionPresenter {

    private QuizQuestion quizQuestion;

    public PictureSelectionQuizPresenter(QuizQuestion quizQuestion) {
        this.quizQuestion = quizQuestion;
    }

    @Override
    public String generateInstruction() {
        return "";
    }

}