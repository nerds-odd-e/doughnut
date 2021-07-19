package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

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

}
