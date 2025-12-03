package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerableMCQ;
import com.odde.doughnut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {
  private final EntityPersister entityPersister;

  public AnswerService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public Answer createAnswerForQuestion(AnswerableMCQ answerableMCQ, AnswerDTO answerDTO) {
    Answer answer = answerableMCQ.buildAnswer(answerDTO);
    entityPersister.save(answerableMCQ);
    return answer;
  }
}
