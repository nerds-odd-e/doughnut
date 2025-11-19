package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.RecallQuestionService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recall-prompts")
class RecallPromptController {
  private final CurrentUser currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final RecallQuestionService recallQuestionService;
  private final AuthorizationService authorizationService;

  public RecallPromptController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      CurrentUser currentUser,
      TestabilitySettings testabilitySettings,
      ObjectMapper objectMapper,
      AuthorizationService authorizationService) {
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.recallQuestionService =
        new RecallQuestionService(
            openAiApi, modelFactoryService, testabilitySettings.getRandomizer(), objectMapper);
  }

  @GetMapping("/{memoryTracker}/question")
  @Transactional
  public RecallPrompt askAQuestion(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    return recallQuestionService.generateAQuestion(memoryTracker);
  }

  @PostMapping("/{recallPrompt}/regenerate")
  @Transactional
  public RecallPrompt regenerate(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
      @RequestBody QuestionContestResult contestResult)
      throws JsonProcessingException {
    authorizationService.assertLoggedIn(currentUser.getUser());
    return recallQuestionService.regenerateAQuestion(
        contestResult,
        recallPrompt.getPredefinedQuestion().getNote(),
        recallPrompt.getPredefinedQuestion().getMcqWithAnswer());
  }

  @PostMapping("/{recallPrompt}/contest")
  @Transactional
  public QuestionContestResult contest(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    return recallQuestionService.contest(recallPrompt);
  }

  @PostMapping("/{recallPrompt}/answer")
  @Transactional
  public AnsweredQuestion answerQuiz(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
      @Valid @RequestBody AnswerDTO answerDTO) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    return recallQuestionService.answerQuestion(
        recallPrompt,
        answerDTO,
        currentUser.getUser(),
        testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping(path = "/{recallPrompt}")
  @Transactional
  public AnsweredQuestion showQuestion(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(currentUser.getUser(), recallPrompt);
    return recallPrompt.getAnsweredQuestion();
  }
}
