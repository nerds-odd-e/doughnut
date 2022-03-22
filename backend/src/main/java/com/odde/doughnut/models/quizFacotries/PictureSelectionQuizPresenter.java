package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;

public class PictureSelectionQuizPresenter implements QuizQuestionPresenter {

    private ReviewPoint reviewPoint;

    public PictureSelectionQuizPresenter(QuizQuestion quizQuestion) {
        this.reviewPoint = quizQuestion.getReviewPoint();
    }

    @Override
    public String mainTopic() {
        return reviewPoint.getNote().getTitle();
    }

    @Override
    public String instruction() {
        return "";
    }

    @Override
    public QuizQuestionViewedByUser.OptionCreator optionCreator() {
        return new QuizQuestionViewedByUser.PictureOptionCreator();
    }

}