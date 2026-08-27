package com.odde.donut.services;

import com.odde.donut.controllers.dto.AnswerDTO;
import com.odde.donut.entities.Answer;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {
  private final EntityPersister entityPersister;

  public AnswerService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public Answer createAnswerForQuestion(
      RecallPrompt recallPrompt, AnswerDTO answerDTO, Timestamp currentUTCTimestamp) {
    Answer answer =
        Answer.buildAnswer(
            answerDTO, recallPrompt.getMcq(), recallPrompt.getAnswer(), currentUTCTimestamp);
    entityPersister.save(answer);
    recallPrompt.setAnswer(answer);
    entityPersister.save(recallPrompt);
    return answer;
  }
}
