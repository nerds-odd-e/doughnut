package com.odde.doughnut.entities;

import com.odde.doughnut.controllers.json.QuizQuestion;
import org.springframework.lang.Nullable;

public class AnsweredQuestion {
  public Integer answerId;
  public boolean correct;
  @Nullable public Integer correctChoiceIndex;
  @Nullable public Integer choiceIndex;
  public String answerDisplay;
  @Nullable public ReviewPoint reviewPoint;
  public QuizQuestion quizQuestion;
}
