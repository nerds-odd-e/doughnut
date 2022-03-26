package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.entities.json.ReviewPointViewedByUser;

public class AnswerViewedByUser {
    public Integer answerId;
    public String answerDisplay;
    public boolean correct;
    public ReviewPointViewedByUser reviewPoint;
    public QuizQuestionViewedByUser quizQuestion;
}
