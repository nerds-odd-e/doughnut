package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.controllers.dto.QuestionSuggestionParams;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class SuggestedQuestionForFineTuningService {
  private final EntityPersister entityPersister;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  public SuggestedQuestionForFineTuningService(
      EntityPersister entityPersister,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer) {
    this.entityPersister = entityPersister;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
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
    RetrievalConfig config =
        RetrievalConfig.forQuestionGeneration(predefinedQuestion.getContextSeed());
    FocusContextResult focusContextResult =
        focusContextRetrievalService.retrieve(predefinedQuestion.getNote(), user, config);
    entity.setPreservedNoteContent(focusContextMarkdownRenderer.render(focusContextResult, config));
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
