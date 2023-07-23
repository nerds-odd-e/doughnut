package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.QuizQuestion;
import org.springframework.lang.Nullable;

public class AnswerViewedByUser {
  public Integer answerId;
  public boolean correct;
  public String answerDisplay;
  @Nullable public ReviewPoint reviewPoint;
  public QuizQuestion quizQuestion;
}
