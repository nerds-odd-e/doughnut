package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;
import java.util.Optional;

@Entity
@DiscriminatorValue("2")
public class QuizQuestionSpelling extends QuizQuestionEntity {

  @Override
  public Integer getCorrectAnswerIndex() {
    return null;
  }

  public List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService) {
    return List.of();
  }

  public String getStem() {
    return getNote().getClozeDescription().clozeDetails();
  }

  public String getMainTopic() {
    return "";
  }

  public Optional<PictureWithMask> getPictureWithMask() {
    return Optional.empty();
  }

  @Override
  public boolean checkAnswer(Answer answer) {
    return getNote().matchAnswer(answer.getSpellingAnswer());
  }
}
