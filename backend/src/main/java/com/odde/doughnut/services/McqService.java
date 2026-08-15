package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import java.sql.Timestamp;
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

  public Mcq addQuestion(Note note, Mcq mcq) {
    mcq.setNote(note);

    Notebook parentNotebook = note.getNotebook();
    parentNotebook.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
    entityPersister.save(parentNotebook);
    entityPersister.save(mcq);
    return mcq;
  }

  public Mcq refineAIQuestion(Note note, Mcq mcq) {
    GeneratedMcq aiGeneratedRefineQuestion =
        aiQuestionGenerator.getAiGeneratedRefineQuestion(note, mcq);
    if (aiGeneratedRefineQuestion == null) {
      return null;
    }
    return aiGeneratedRefineQuestion.toMcq(note);
  }

  public QuestionContestResult contest(Mcq mcq) {
    QuestionEvaluation questionContestResult =
        aiQuestionGenerator.getQuestionContestResult(mcq.getNote(), mcq);
    if (questionContestResult == null) {
      return null;
    }
    QuestionContestResult result = questionContestResult.getQuestionContestResult(mcq);
    if (!result.rejected) {
      mcq.setContested(true);
      entityPersister.merge(mcq);
    }
    return result;
  }

  public Mcq generateAFeasibleQuestion(Note note) {
    return generateAFeasibleQuestion(note, null);
  }

  public Mcq generateAFeasibleQuestion(Note note, String propertyKey) {
    Long contextSeedBoxed = Long.valueOf(ThreadLocalRandom.current().nextLong());
    GeneratedMcq generatedMcq =
        aiQuestionGenerator.getAiGeneratedQuestion(note, null, contextSeedBoxed, propertyKey);
    if (generatedMcq == null) {
      return null;
    }

    Mcq result = generatedMcq.toMcq(note, contextSeedBoxed);
    entityPersister.save(result);

    for (int i = 0; i < regenerationTimes; i++) {
      QuestionContestResult contestResult = contest(result);

      if (contestResult == null || contestResult.rejected) {
        return result;
      }

      Long regSeedBoxed = Long.valueOf(ThreadLocalRandom.current().nextLong());
      GeneratedMcq regeneratedQuestion =
          aiQuestionGenerator.regenerateQuestion(
              contestResult, note, result, regSeedBoxed, propertyKey);
      if (regeneratedQuestion != null) {
        Mcq regenerated = regeneratedQuestion.toMcq(note, regSeedBoxed);
        result = entityPersister.save(regenerated);
      } else {
        return result;
      }
    }

    return result;
  }
}
