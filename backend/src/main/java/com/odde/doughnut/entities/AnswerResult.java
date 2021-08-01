package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.*;

public class AnswerResult {
    @Getter
    @Setter
    String answer;

    @Getter
    @Setter
    Note answerNote;

    @Getter
    @Setter
    @JsonIgnore
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
        List<Note> wrongAnswers = questionType.factory.apply(reviewPoint).knownWrongAnswers();
        if (wrongAnswers != null) {
            return wrongAnswers.stream().noneMatch(this::matchAnswer);
        }
        List<Note> rightAnswers = questionType.factory.apply(reviewPoint).knownRightAnswers();
        return rightAnswers.stream().anyMatch(this::matchAnswer);
    }

    private boolean matchAnswer(Note correctAnswerNote) {
        if (answerNote != null) {
            return correctAnswerNote.equals(answerNote);
        }

        return correctAnswerNote.getNoteContent().getNoteTitle().matches(answer);
    }

}
