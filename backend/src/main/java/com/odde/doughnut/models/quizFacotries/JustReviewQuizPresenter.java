package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;

import java.util.Optional;

public class JustReviewQuizPresenter implements QuizQuestionPresenter {
    private final ReviewPoint reviewPoint;

    public JustReviewQuizPresenter(QuizQuestion quizQuestion) {
        this.reviewPoint = quizQuestion.getReviewPoint();
    }

    @Override
    public Optional<Integer> revealedNoteId() {
        return Optional.of(reviewPoint.getSourceNote().getId());
    }

    @Override
    public String mainTopic() {
        return "";
    }

    @Override
    public String instruction() {
        return "";
    }

}
