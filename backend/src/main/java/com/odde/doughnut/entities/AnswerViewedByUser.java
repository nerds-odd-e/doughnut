package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.QuizQuestion;

public class AnswerViewedByUser {
  public Integer answerId;
  public String answerDisplay;
  public boolean correct;
  public ReviewPoint reviewPoint;
  public QuizQuestion quizQuestion;
}
