package com.odde.doughnut.entities;

import com.odde.doughnut.controllers.json.QuizQuestion;

public class AnsweredQuestion {
  public Integer answerId;
  public boolean correct;
  public Integer correctChoiceIndex;
  public Integer choiceIndex;
  public String answerDisplay;
  public ReviewPoint reviewPoint;
  public QuizQuestion quizQuestion;
}
