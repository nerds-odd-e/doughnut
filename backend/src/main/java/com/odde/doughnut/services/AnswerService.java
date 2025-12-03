package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {
  private final EntityPersister entityPersister;

  public AnswerService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public Answer createAnswerForQuestion(RecallPrompt recallPrompt, AnswerDTO answerDTO) {
    Answer answer =
        Answer.buildAnswer(
            answerDTO, recallPrompt.getPredefinedQuestion(), recallPrompt.getAnswer());
    recallPrompt.setAnswer(answer);
    entityPersister.save(recallPrompt);
    return answer;
  }

  public Answer createAnswerForQuestion(
      AssessmentQuestionInstance assessmentQuestionInstance, AnswerDTO answerDTO) {
    Answer answer = assessmentQuestionInstance.buildAnswer(answerDTO);
    entityPersister.save(assessmentQuestionInstance);
    return answer;
  }
}
