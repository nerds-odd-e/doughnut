package com.odde.donut.services;

import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.*;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.ai.AiQuestionGenerator;
import com.odde.donut.services.ai.QuestionEvaluation;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class McqService {
  private final EntityPersister entityPersister;
  private final AiQuestionGenerator aiQuestionGenerator;
  private final int regenerationTimes;

  @Autowired
  public McqService(
      EntityPersister entityPersister,
      AiQuestionGenerator aiQuestionGenerator,
      @Value("${question.regeneration.times:0}") int regenerationTimes) {
    this.entityPersister = entityPersister;
    this.aiQuestionGenerator = aiQuestionGenerator;
    this.regenerationTimes = regenerationTimes;
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

  public Mcq generateAFeasibleQuestion(Note note) {
    return generateAFeasibleQuestion(note, null);
  }

  public Mcq generateAFeasibleQuestion(Note note, PropertyFocus propertyFocus) {
    Long contextSeedBoxed = Long.valueOf(ThreadLocalRandom.current().nextLong());
    Mcq result =
        aiQuestionGenerator.getAiGeneratedQuestion(note, null, contextSeedBoxed, propertyFocus);
    if (result == null) {
      return null;
    }

    for (int i = 0; i < regenerationTimes; i++) {
      QuestionContestResult contestResult = evaluate(note, result);

      if (contestResult == null || contestResult.rejected) {
        return result;
      }

      Long regSeedBoxed = Long.valueOf(ThreadLocalRandom.current().nextLong());
      Mcq regeneratedQuestion =
          aiQuestionGenerator.regenerateQuestion(
              contestResult, note, result, regSeedBoxed, propertyFocus);
      if (regeneratedQuestion != null) {
        result = regeneratedQuestion;
      } else {
        return result;
      }
    }

    return result;
  }
}
