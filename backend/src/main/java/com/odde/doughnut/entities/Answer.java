package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.*;

public class Answer {
    @NotNull
    @Getter
    @Setter
    String answer;

    @Getter
    @Setter
    ReviewPoint reviewPoint;

    @Getter
    @Setter
    QuizQuestion.QuestionType questionType;

    public boolean checkAnswer() {
        if (questionType == LINK_TARGET) {
            return (answer.equals(reviewPoint.getLink().getTargetNote().getTitle()));
        }
        if (questionType == LINK_SOURCE_EXCLUSIVE) {
            return reviewPoint.getLink().getBackwardPeers().stream()
                    .map(Note::getTitle).noneMatch(t->t.equals(answer));
        }
        return (
                answer.toLowerCase().equals(
                        reviewPoint.getNote().getTitle().toLowerCase().trim()));
    }
}
