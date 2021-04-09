package com.odde.doughnut.entities;

import com.odde.doughnut.models.QuizQuestion;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.*;

public class Answer {
    @NotNull
    @Getter
    @Setter
    String answer;

    @Getter
    @Setter
    ReviewPointEntity reviewPointEntity;

    @Getter
    @Setter
    QuizQuestion.QuestionType questionType;

    public boolean checkAnswer() {
        if (questionType == LINK_TARGET) {
            return (answer.equals(reviewPointEntity.getLink().getTargetNote().getTitle()));
        }
        if (questionType == LINK_SOURCE_EXCLUSIVE) {
            return reviewPointEntity.getLink().getBackwardPeers().stream()
                    .map(Note::getTitle).noneMatch(t->t.equals(answer));
        }
        return (
                answer.toLowerCase().trim().equals(
                        reviewPointEntity.getNote().getTitle().toLowerCase().trim()));
    }
}
