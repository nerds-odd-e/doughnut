package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    Integer answerNoteId;

    @Getter
    @Setter
    QuizQuestion.QuestionType questionType;

}
