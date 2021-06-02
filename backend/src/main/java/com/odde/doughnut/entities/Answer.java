package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_TARGET;

public class Answer {
    @Getter
    @Setter
    String answer;

    @Getter
    @Setter
    Note answerNote;

    @Getter
    @Setter
    ReviewPoint reviewPoint;

    @Getter
    @Setter
    QuizQuestion.QuestionType questionType;

    public String getAnswerDisplay() {
        if (answerNote != null) {
            return answerNote.getTitle();
        }
        return answer;
    }

    public boolean isCorrect() {
        if (questionType == LINK_TARGET) {
            return (matchAnswer(reviewPoint.getLink().getTargetNote()));
        }
        if (questionType == LINK_SOURCE_EXCLUSIVE) {
            return reviewPoint.getLink().getBackwardPeers().stream()
                    .noneMatch(this::matchAnswer);
        }
        return matchAnswer(reviewPoint.getNote());
    }

    private boolean matchAnswer(Note matchingNote) {
        if (answerNote != null) {
            return matchingNote.equals(answerNote);
        }

        return matchingNote.getTitle().toLowerCase().trim().equals(answer.toLowerCase());
    }
}
