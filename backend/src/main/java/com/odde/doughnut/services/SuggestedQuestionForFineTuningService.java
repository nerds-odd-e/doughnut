package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.controllers.dto.QuestionSuggestionParams;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class SuggestedQuestionForFineTuningService {
  private final EntityManager entityManager;

  public SuggestedQuestionForFineTuningService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public SuggestedQuestionForFineTuning update(
      SuggestedQuestionForFineTuning entity, QuestionSuggestionParams params) {
    entity.preserveQuestion(params.preservedQuestion);
    entity.setPreservedNoteContent(params.preservedNoteContent);
    entity.setComment(params.comment);
    entity.setPositiveFeedback(params.positiveFeedback);
    entity.setRealCorrectAnswers(params.realCorrectAnswers);
    return entityManager.merge(entity);
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
    if (entity.getId() == null) {
      entityManager.persist(entity);
      return entity;
    }
    return entityManager.merge(entity);
  }

  public SuggestedQuestionForFineTuning duplicate(SuggestedQuestionForFineTuning entity) {
    SuggestedQuestionForFineTuning newObject = new SuggestedQuestionForFineTuning();
    newObject.setUser(entity.getUser());
    newObject.preserveQuestion(entity.getPreservedQuestion());
    newObject.setPreservedNoteContent(entity.getPreservedNoteContent());
    newObject.setComment(entity.getComment());
    newObject.setPositiveFeedback(entity.isPositiveFeedback());
    entityManager.persist(newObject);
    return newObject;
  }

  public SuggestedQuestionForFineTuning delete(SuggestedQuestionForFineTuning entity) {
    entityManager.remove(entity);
    return entity;
  }
}
