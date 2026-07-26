package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.PredefinedQuestionRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PredefinedQuestionService {
  private final EntityPersister entityPersister;
  private final AiQuestionGenerator aiQuestionGenerator;
  private final PredefinedQuestionRepository predefinedQuestionRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final int regenerationTimes;

  @Autowired
  public PredefinedQuestionService(
      EntityPersister entityPersister,
      AiQuestionGenerator aiQuestionGenerator,
      PredefinedQuestionRepository predefinedQuestionRepository,
      RecallPromptRepository recallPromptRepository,
      @Value("${question.regeneration.times:0}") int regenerationTimes) {
    this.entityPersister = entityPersister;
    this.aiQuestionGenerator = aiQuestionGenerator;
    this.predefinedQuestionRepository = predefinedQuestionRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.regenerationTimes = regenerationTimes;
  }

  public PredefinedQuestion addQuestion(Note note, PredefinedQuestion predefinedQuestion) {
    predefinedQuestion.setNote(note);

    Notebook parentNotebook = note.getNotebook();
    parentNotebook.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
    entityPersister.save(parentNotebook);
    entityPersister.save(predefinedQuestion);
    return predefinedQuestion;
  }

  public void deleteQuestions(Note note, List<Integer> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    Set<Integer> requestedIds = new HashSet<>(ids);
    List<PredefinedQuestion> questions =
        predefinedQuestionRepository.findByIdInAndNote_Id(ids, note.getId());
    if (questions.size() != requestedIds.size()) {
      throw new ApiException(
          "Questions do not belong to note",
          ApiError.ErrorType.BINDING_ERROR,
          "Delete failed: One or more questions do not belong to this note.");
    }
    List<RecallPrompt> prompts = recallPromptRepository.findByPredefinedQuestion_IdIn(ids);
    for (RecallPrompt prompt : prompts) {
      prompt.setPredefinedQuestion(null);
      entityPersister.save(prompt);
    }
    for (PredefinedQuestion question : questions) {
      entityPersister.remove(question);
    }
    Notebook parentNotebook = note.getNotebook();
    parentNotebook.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
    entityPersister.save(parentNotebook);
  }

  public PredefinedQuestion refineAIQuestion(Note note, PredefinedQuestion predefinedQuestion) {
    MCQWithAnswer aiGeneratedRefineQuestion =
        aiQuestionGenerator.getAiGeneratedRefineQuestion(
            note, predefinedQuestion.getMcqWithAnswer());
    if (aiGeneratedRefineQuestion == null) {
      return null;
    }
    return PredefinedQuestion.fromMCQWithAnswer(aiGeneratedRefineQuestion, note);
  }

  public QuestionContestResult contest(PredefinedQuestion predefinedQuestion) {
    MCQWithAnswer mcqWithAnswer = predefinedQuestion.getMcqWithAnswer();
    QuestionEvaluation questionContestResult =
        aiQuestionGenerator.getQuestionContestResult(predefinedQuestion.getNote(), mcqWithAnswer);
    if (questionContestResult == null) {
      return null;
    }
    QuestionContestResult result = questionContestResult.getQuestionContestResult(mcqWithAnswer);
    if (!result.rejected) {
      predefinedQuestion.setContested(true);
      entityPersister.merge(predefinedQuestion);
    }
    return result;
  }

  public PredefinedQuestion generateAFeasibleQuestion(Note note) {
    return generateAFeasibleQuestion(note, null);
  }

  public PredefinedQuestion generateAFeasibleQuestion(Note note, String propertyKey) {
    Long contextSeedBoxed = Long.valueOf(ThreadLocalRandom.current().nextLong());
    MCQWithAnswer mcqWithAnswer =
        aiQuestionGenerator.getAiGeneratedQuestion(note, null, contextSeedBoxed, propertyKey);
    if (mcqWithAnswer == null) {
      return null;
    }

    PredefinedQuestion result =
        PredefinedQuestion.fromMCQWithAnswer(mcqWithAnswer, note, contextSeedBoxed);
    entityPersister.save(result);

    // Auto-evaluate and regenerate up to regenerationTimes
    for (int i = 0; i < regenerationTimes; i++) {
      QuestionContestResult contestResult = contest(result);

      if (contestResult == null || contestResult.rejected) {
        return result;
      }

      Long regSeedBoxed = Long.valueOf(ThreadLocalRandom.current().nextLong());
      MCQWithAnswer regeneratedQuestion =
          aiQuestionGenerator.regenerateQuestion(
              contestResult, note, mcqWithAnswer, regSeedBoxed, propertyKey);
      if (regeneratedQuestion != null) {
        PredefinedQuestion regenerated =
            PredefinedQuestion.fromMCQWithAnswer(regeneratedQuestion, note, regSeedBoxed);
        result = entityPersister.save(regenerated);
        mcqWithAnswer = regeneratedQuestion;
      } else {
        return result;
      }
    }

    return result;
  }
}
