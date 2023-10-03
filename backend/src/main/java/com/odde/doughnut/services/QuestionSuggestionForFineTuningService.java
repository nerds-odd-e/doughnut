package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.QuestionSuggestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
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
    suggestedQuestionForFineTuning.setPreservedQuestion(suggestion.mcqWithAnswer.toJsonString());

    return modelFactoryService.questionSuggestionForFineTuningRepository.save(
        suggestedQuestionForFineTuning);
  }
}
