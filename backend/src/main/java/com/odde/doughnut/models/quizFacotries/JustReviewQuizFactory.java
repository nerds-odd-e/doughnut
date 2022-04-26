package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.ReviewPoint;

public record JustReviewQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant)
    implements QuizQuestionFactory {}
