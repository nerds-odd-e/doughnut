package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class Answer {
    @Getter
    @Setter
    String answer;

    @Getter
    @Setter
    Integer answerNoteId;

    @Getter
    @Setter
    QuizQuestion.QuestionType questionType;

    @Getter
    @Setter
    QuizQuestion question;

    @Getter @Setter
    private List<Integer> viceReviewPointIds;
}
