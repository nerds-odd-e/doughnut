package com.odde.doughnut.entities;

import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.entities.json.ReviewPointWithReviewSetting;

public class AnswerViewedByUser {
  public Integer answerId;
  public String answerDisplay;
  public boolean correct;
  public ReviewPointWithReviewSetting reviewPoint;
  public QuizQuestionViewedByUser quizQuestion;
}
