package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.json.QuestionSuggestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AIGeneratedQuestion;
import java.sql.Timestamp;

public record QuestionSuggestionForFineTuningService() {

  public SuggestedQuestionForFineTuning suggestQuestion(
      QuizQuestionEntity quizQuestionEntity,
      QuestionSuggestion suggestion,
      User user,
      Timestamp currentUTCTimestamp,
      ModelFactoryService modelFactoryService) {
    SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
        new SuggestedQuestionForFineTuning();
    suggestedQuestionForFineTuning.setUser(user);
    suggestedQuestionForFineTuning.setQuizQuestion(quizQuestionEntity);
    suggestedQuestionForFineTuning.setComment(suggestion.comment);
    suggestedQuestionForFineTuning.setCreatedAt(currentUTCTimestamp);

    updateQuestionStemWithSuggestion(
        suggestion.suggestion, quizQuestionEntity, modelFactoryService);

    return modelFactoryService.questionSuggestionForFineTuningRepository.save(
        suggestedQuestionForFineTuning);
  }

  private static void updateQuestionStemWithSuggestion(
      String suggestion,
      QuizQuestionEntity quizQuestionEntity,
      ModelFactoryService modelFactoryService) {
    if (suggestion != null && !suggestion.isEmpty()) {

      AIGeneratedQuestion aiGeneratedQuestion = null;
      try {
        aiGeneratedQuestion =
            new ObjectMapper()
                .readValue(quizQuestionEntity.getRawJsonQuestion(), AIGeneratedQuestion.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      aiGeneratedQuestion.stem = suggestion;
      quizQuestionEntity.setRawJsonQuestion(aiGeneratedQuestion.toJsonString());

      modelFactoryService.quizQuestionRepository.save(quizQuestionEntity);
    }
  }
}
