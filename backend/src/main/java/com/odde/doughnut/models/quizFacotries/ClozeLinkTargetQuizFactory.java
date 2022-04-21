package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.ReviewPoint;

public class ClozeLinkTargetQuizFactory extends LinkTargetQuizFactory {

  public ClozeLinkTargetQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
  }
}
