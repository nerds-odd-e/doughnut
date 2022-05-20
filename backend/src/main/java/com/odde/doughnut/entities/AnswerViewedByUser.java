package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;

public class AnswerViewedByUser {
  public Integer answerId;
  public String answerDisplay;
  public boolean correct;
  public ReviewPoint reviewPoint;
  public QuizQuestionViewedByUser quizQuestion;
}
