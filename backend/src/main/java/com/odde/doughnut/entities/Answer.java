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
            return (matchAnswer(reviewPoint.getLink().getTargetNote()));
        }
        if (questionType == LINK_SOURCE_EXCLUSIVE) {
            return reviewPoint.getLink().getBackwardPeers().stream()
                    .noneMatch(this::matchAnswer);
        }
        return matchAnswer(reviewPoint.getNote());
    }

    private boolean matchAnswer(Note note) {
        return answer.toLowerCase().equals(
                note.getTitle().toLowerCase().trim());
    }
}
