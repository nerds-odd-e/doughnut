package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.AuthorizationService;
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

  @Autowired
  public RecallPromptController(
      RecallQuestionService recallQuestionService,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService) {
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.recallQuestionService = recallQuestionService;
  }

  @GetMapping("/{memoryTracker}/question")
  @Transactional
  public PredefinedQuestion askAQuestion(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker) {
    authorizationService.assertLoggedIn();
    return recallQuestionService.generateAQuestion(memoryTracker);
  }

  @PostMapping("/{predefinedQuestion}/regenerate")
  @Transactional
  public PredefinedQuestion regenerate(
      @PathVariable("predefinedQuestion") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion,
      @RequestBody QuestionContestResult contestResult)
      throws JsonProcessingException {
    authorizationService.assertLoggedIn();
    return recallQuestionService.regenerateAQuestion(
        contestResult, predefinedQuestion.getNote(), predefinedQuestion.getMcqWithAnswer());
  }

  @PostMapping("/{predefinedQuestion}/contest")
  @Transactional
  public QuestionContestResult contest(
      @PathVariable("predefinedQuestion") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion) {
    authorizationService.assertLoggedIn();
    return recallQuestionService.contest(predefinedQuestion);
  }

  @PostMapping("/{predefinedQuestion}/answer")
  @Transactional
  public AnsweredQuestion answerQuiz(
      @PathVariable("predefinedQuestion") @Schema(type = "integer")
          PredefinedQuestion predefinedQuestion,
      @Valid @RequestBody AnswerDTO answerDTO) {
    authorizationService.assertLoggedIn();
    return recallQuestionService.answerQuestion(
        predefinedQuestion,
        answerDTO,
        authorizationService.getCurrentUser(),
        testabilitySettings.getCurrentUTCTimestamp());
  }
}
