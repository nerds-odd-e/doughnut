package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.controllers.dto.QuestionSuggestionParams;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class SuggestedQuestionForFineTuningService {
  private final EntityPersister entityPersister;

  public SuggestedQuestionForFineTuningService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public SuggestedQuestionForFineTuning update(
      SuggestedQuestionForFineTuning entity, QuestionSuggestionParams params) {
    entity.preserveQuestion(params.preservedQuestion);
    entity.setPreservedNoteContent(params.preservedNoteContent);
    entity.setComment(params.comment);
    entity.setPositiveFeedback(params.positiveFeedback);
    entity.setRealCorrectAnswers(params.realCorrectAnswers);
    return entityPersister.merge(entity);
  }

  public SuggestedQuestionForFineTuning suggestQuestionForFineTuning(
      SuggestedQuestionForFineTuning entity,
      PredefinedQuestion predefinedQuestion,
      QuestionSuggestionCreationParams suggestionCreationParams,
      User user,
      Timestamp currentUTCTimestamp) {
    entity.setUser(user);
    entity.setCreatedAt(currentUTCTimestamp);
    entity.preserveNoteContent(predefinedQuestion.getNote());
    entity.preserveQuestion(predefinedQuestion.getMcqWithAnswer());
    entity.setComment(suggestionCreationParams.comment);
    entity.setPositiveFeedback(suggestionCreationParams.isPositiveFeedback);
    if (suggestionCreationParams.isPositiveFeedback) {
      entity.setRealCorrectAnswers("%d".formatted(predefinedQuestion.getCorrectAnswerIndex()));
    }
    return entityPersister.save(entity);
  }

  public SuggestedQuestionForFineTuning duplicate(SuggestedQuestionForFineTuning entity) {
    SuggestedQuestionForFineTuning newObject = new SuggestedQuestionForFineTuning();
    newObject.setUser(entity.getUser());
    newObject.preserveQuestion(entity.getPreservedQuestion());
    newObject.setPreservedNoteContent(entity.getPreservedNoteContent());
    newObject.setComment(entity.getComment());
    newObject.setPositiveFeedback(entity.isPositiveFeedback());
    entityPersister.save(newObject);
    return newObject;
  }

  public SuggestedQuestionForFineTuning delete(SuggestedQuestionForFineTuning entity) {
    entityPersister.remove(entity);
    return entity;
  }
}
