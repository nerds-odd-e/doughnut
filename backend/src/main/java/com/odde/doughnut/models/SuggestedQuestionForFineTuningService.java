package com.odde.doughnut.models;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public class SuggestedQuestionForFineTuningService {
  private final SuggestedQuestionForFineTuning entity;
  private final ModelFactoryService modelFactoryService;

  public SuggestedQuestionForFineTuningService(
      SuggestedQuestionForFineTuning suggestion, ModelFactoryService modelFactoryService) {
    this.entity = suggestion;
    this.modelFactoryService = modelFactoryService;
  }

  public SuggestedQuestionForFineTuning create(
      QuizQuestionEntity quizQuestionEntity, User user, Timestamp currentUTCTimestamp) {
    entity.setUser(user);
    entity.setQuizQuestion(quizQuestionEntity);
    entity.setCreatedAt(currentUTCTimestamp);
    return modelFactoryService.questionSuggestionForFineTuningRepository.save(entity);
  }
}
