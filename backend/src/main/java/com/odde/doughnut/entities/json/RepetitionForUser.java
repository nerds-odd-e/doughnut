package com.odde.doughnut.entities.json;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

public class RepetitionForUser {
    @Getter
    @Setter
    private ReviewPointViewedByUser reviewPointViewedByUser;
    @Getter
    @Setter
    private QuizQuestionViewedByUser quizQuestion;
    @Getter
    @Setter
    private Integer toRepeatCount;
}
