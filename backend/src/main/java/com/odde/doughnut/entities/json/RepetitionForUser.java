package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.QuizQuestion;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

public class RepetitionForUser {
    @Getter
    @Setter
    private ReviewPointViewedByUser reviewPointViewedByUser;
    @Getter
    @Setter
    private Optional<QuizQuestionViewedByUser> quizQuestion;
    @Getter
    @Setter
    private Answer emptyAnswer;
    @Getter
    @Setter
    private Integer toRepeatCount;
}
