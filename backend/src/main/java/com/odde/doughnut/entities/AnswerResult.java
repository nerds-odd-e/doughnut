package com.odde.doughnut.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

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
        List<Note> wrongAnswers = questionType.factory.apply(reviewPoint).allWrongAnswers();
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

        return correctAnswerNote.getNoteTitle().matches(answer);
    }

}
