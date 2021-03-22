package com.odde.doughnut.entities;

import com.odde.doughnut.models.QuizQuestion;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AnswerEntity {
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
        if (questionType == QuizQuestion.QuestionType.LINK_TARGET) {
            return (answer.equals(reviewPointEntity.getLinkEntity().getTargetNote().getTitle()));
        }
        return (
                answer.toLowerCase().trim().equals(
                        reviewPointEntity.getNoteEntity().getTitle().toLowerCase().trim()));
    }
}
