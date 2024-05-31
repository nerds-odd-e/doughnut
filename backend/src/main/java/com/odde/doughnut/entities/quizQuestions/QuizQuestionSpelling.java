package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.QuizQuestionEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("2")
public class QuizQuestionSpelling extends QuizQuestionEntity {

  @Override
  public boolean checkAnswer(Answer answer) {
    return getNote().matchAnswer(answer.getSpellingAnswer());
  }
}
