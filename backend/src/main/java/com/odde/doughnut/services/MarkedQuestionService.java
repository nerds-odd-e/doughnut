package com.odde.doughnut.services;

import com.odde.doughnut.entities.MarkedQuestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public record MarkedQuestionService(
    User user, Timestamp currentUTCTimestamp, ModelFactoryService modelFactoryService) {

  public MarkedQuestion markQuestion(QuizQuestionEntity quizQuestionEntity) {
    MarkedQuestion markedQuestion = new MarkedQuestion();
    markedQuestion.setUserId(user.getId());
    markedQuestion.setQuizQuestionId(quizQuestionEntity.getId());
    markedQuestion.setNoteId(quizQuestionEntity.getThing().getNote().getId());
    return modelFactoryService.markedQuestionRepository.save(markedQuestion);
  }
}
