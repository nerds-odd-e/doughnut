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
    //    markedQuestion.setComment(quizQuestionEntity.getThing().getComment());
    markedQuestion.setCreatedAt(currentUTCTimestamp);
    return modelFactoryService.markedQuestionRepository.save(markedQuestion);
  }
}
