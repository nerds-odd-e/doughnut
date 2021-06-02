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
        if (questionType == LINK_SOURCE_EXCLUSIVE) {
            return reviewPoint.getLink().getBackwardPeers().stream()
                    .noneMatch(this::matchAnswer);
        }
        return matchAnswer(getCorrectAnswerNote());
    }

    private Note getCorrectAnswerNote() {
        if (questionType == LINK_TARGET) {
            return reviewPoint.getLink().getTargetNote();
        }
        return reviewPoint.getNote();
    }

    private boolean matchAnswer(Note correctAnswerNote) {
        if (answerNote != null) {
            return correctAnswerNote.equals(answerNote);
        }

        return correctAnswerNote.getNoteContent().getNoteTitle().matches(answer);
    }

}
