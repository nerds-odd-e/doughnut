package com.odde.doughnut.entities;

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

    public boolean correct;
}
