package com.odde.doughnut.services;

import com.odde.doughnut.entities.MarkedQuestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public record MarkedQuestionService() {

  public MarkedQuestion markQuestion(
      QuizQuestionEntity quizQuestionEntity,
      User user,
      Timestamp currentUTCTimestamp,
      ModelFactoryService modelFactoryService) {
    MarkedQuestion markedQuestion = new MarkedQuestion();
    markedQuestion.setUserId(user.getId());
    markedQuestion.setQuizQuestion(quizQuestionEntity);
    markedQuestion.setNote(quizQuestionEntity.getThing().getNote());
    markedQuestion.setCreatedAt(currentUTCTimestamp);

    updateQuestionStemWithSuggestion(quizQuestionEntity, modelFactoryService);

    return modelFactoryService.markedQuestionRepository.save(markedQuestion);
  }

  private static void updateQuestionStemWithSuggestion(
      QuizQuestionEntity quizQuestionEntity, ModelFactoryService modelFactoryService) {
    if (quizQuestionEntity.getRawJsonQuestion() != null
        && quizQuestionEntity.getRawJsonQuestion().contains("Blah blah blah")) {
      quizQuestionEntity.setRawJsonQuestion("Who wrote 'Who Let the Cats Out'?");
      modelFactoryService.quizQuestionRepository.save(quizQuestionEntity);
    }
  }
}
