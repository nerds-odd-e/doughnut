package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerableQuestionInstance;
import com.odde.doughnut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {
  private final EntityPersister entityPersister;

  public AnswerService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public Answer createAnswerForQuestion(
      AnswerableQuestionInstance answerableQuestionInstance, AnswerDTO answerDTO) {
    Answer answer = answerableQuestionInstance.buildAnswer(answerDTO);
    entityPersister.save(answerableQuestionInstance);
    return answer;
  }
}
