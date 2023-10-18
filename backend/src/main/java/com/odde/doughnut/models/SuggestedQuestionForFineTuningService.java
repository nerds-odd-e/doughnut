package com.odde.doughnut.models;

import com.odde.doughnut.controllers.json.QuestionSuggestionCreationParams;
import com.odde.doughnut.controllers.json.QuestionSuggestionParams;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.FeedbackExistingException;
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
      QuizQuestionEntity quizQuestionEntity, QuestionSuggestionCreationParams params) {
    entity.setQuizQuestion(quizQuestionEntity);
    entity.setPreservedQuestion(quizQuestionEntity.getMcqWithAnswer());
    entity.setComment(params.comment);
    entity.setPositiveFeedback(params.isPositiveFeedback);
    entity.setDuplicated(params.isDuplicated);
    return save();
  }

  public SuggestedQuestionForFineTuning update(QuestionSuggestionParams params) {
    entity.setPreservedQuestion(params.preservedQuestion);
    entity.setComment(params.comment);
    return save();
  }

  private SuggestedQuestionForFineTuning save() {
    return modelFactoryService.questionSuggestionForFineTuningRepository.save(entity);
  }

  public SuggestedQuestionForFineTuning suggestQuestionForFineTuning(
      QuizQuestionEntity quizQuestion,
      QuestionSuggestionCreationParams suggestionCreationParams,
      User user,
      Timestamp currentUTCTimestamp) {
    Integer countByIdAndUserId =
        modelFactoryService.questionSuggestionForFineTuningRepository.countByIdAndUserId(
            quizQuestion.getId(), user.getId());
    entity.setUser(user);
    entity.setCreatedAt(currentUTCTimestamp);

    if (countByIdAndUserId != 0) {
      throw new FeedbackExistingException();
    }
    create(quizQuestion, suggestionCreationParams);

    return entity;
  }

  public SuggestedQuestionForFineTuning duplicate() {
    SuggestedQuestionForFineTuning newObject = new SuggestedQuestionForFineTuning();
    newObject.setUser(entity.getUser());
    newObject.setQuizQuestion(entity.getQuizQuestion());
    newObject.setPreservedQuestion(entity.getPreservedQuestion());
    newObject.setComment(entity.getComment());
    newObject.setPositiveFeedback(entity.isPositiveFeedback());
    newObject.setDuplicated(true);
    modelFactoryService.questionSuggestionForFineTuningRepository.save(newObject);
    return newObject;
  }
}
