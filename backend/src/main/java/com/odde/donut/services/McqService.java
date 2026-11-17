package com.odde.donut.services;

import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.*;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.ai.AiQuestionGenerator;
import com.odde.donut.services.ai.SupportedChoices;
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
    SupportedChoices supported = aiQuestionGenerator.supportedChoices(note, mcq);
    return supported == null ? null : supported.contestResult(mcq);
  }
}
