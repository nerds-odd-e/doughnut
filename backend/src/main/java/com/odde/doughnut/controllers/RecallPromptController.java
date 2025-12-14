package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.controllers.dto.RecallResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.RecallQuestionService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recall-prompts")
class RecallPromptController {

  private final TestabilitySettings testabilitySettings;

  private final RecallQuestionService recallQuestionService;
  private final AuthorizationService authorizationService;
  private final MemoryTrackerService memoryTrackerService;

  @Autowired
  public RecallPromptController(
      RecallQuestionService recallQuestionService,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      MemoryTrackerService memoryTrackerService) {
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.recallQuestionService = recallQuestionService;
    this.memoryTrackerService = memoryTrackerService;
  }

  @PostMapping("/{recallPrompt}/regenerate")
  @Transactional
  public RecallPrompt regenerate(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
      @RequestBody QuestionContestResult contestResult)
      throws JsonProcessingException {
    authorizationService.assertLoggedIn();
    return recallQuestionService.regenerateAQuestion(
        contestResult,
        recallPrompt.getPredefinedQuestion().getNote(),
        recallPrompt.getPredefinedQuestion().getMcqWithAnswer(),
        recallPrompt);
  }

  @PostMapping("/{recallPrompt}/contest")
  @Transactional
  public QuestionContestResult contest(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt) {
    authorizationService.assertLoggedIn();
    return recallQuestionService.contest(recallPrompt);
  }

  @PostMapping("/{recallPrompt}/answer")
  @Transactional
  public RecallResult answerQuiz(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
      @Valid @RequestBody AnswerDTO answerDTO) {
    authorizationService.assertLoggedIn();
    return new RecallResult.QuestionResult(
        recallQuestionService.answerQuestion(
            recallPrompt,
            answerDTO,
            authorizationService.getCurrentUser(),
            testabilitySettings.getCurrentUTCTimestamp()));
  }

  @PostMapping("/{recallPrompt}/answer-spelling")
  @Transactional
  public RecallResult answerSpelling(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
      @Valid @RequestBody AnswerSpellingDTO answerDTO)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(recallPrompt.getMemoryTracker());
    return RecallResult.SpellingResult.from(
        memoryTrackerService.answerSpelling(
            recallPrompt,
            answerDTO,
            authorizationService.getCurrentUser(),
            testabilitySettings.getCurrentUTCTimestamp()));
  }
}
