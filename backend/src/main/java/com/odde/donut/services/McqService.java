package com.odde.donut.services;

import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.*;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.ai.AiQuestionGenerator;
import com.odde.donut.services.ai.QuestionEvaluation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class McqService {
  private final EntityPersister entityPersister;
  private final AiQuestionGenerator aiQuestionGenerator;

  @Autowired
  public McqService(EntityPersister entityPersister, AiQuestionGenerator aiQuestionGenerator) {
    this.entityPersister = entityPersister;
    this.aiQuestionGenerator = aiQuestionGenerator;
  }

  public QuestionContestResult contest(Note note, Mcq mcq) {
    QuestionContestResult result = evaluate(note, mcq);
    if (result != null && !result.rejected) {
      mcq.setContested(true);
      entityPersister.merge(mcq);
    }
    return result;
  }

  private QuestionContestResult evaluate(Note note, Mcq mcq) {
    QuestionEvaluation evaluation = aiQuestionGenerator.getQuestionContestResult(note, mcq);
    return evaluation == null ? null : evaluation.getQuestionContestResult(mcq);
  }
}
