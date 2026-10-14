package com.odde.donut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.controllers.dto.AnswerDTO;
import com.odde.donut.controllers.dto.AnswerSpellingDTO;
import com.odde.donut.controllers.dto.AnsweredQuestion;
import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.MemoryTrackerService;
import com.odde.donut.services.RecallPromptService;
import com.odde.donut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recall-prompts")
class RecallPromptController {

  private final TestabilitySettings testabilitySettings;

  private final RecallPromptService recallPromptService;
  private final AuthorizationService authorizationService;
  private final MemoryTrackerService memoryTrackerService;

  @Autowired
  public RecallPromptController(
      RecallPromptService recallPromptService,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      MemoryTrackerService memoryTrackerService) {
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.recallPromptService = recallPromptService;
    this.memoryTrackerService = memoryTrackerService;
  }

  @PostMapping("/{recallPrompt}/regenerate")
  @Transactional
  public com.odde.donut.controllers.dto.RecallPrompt regenerate(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
      @RequestBody QuestionContestResult contestResult)
      throws JsonProcessingException, UnexpectedNoAccessRightException {
    assertCanMutateRecallPrompt(recallPrompt);
    RecallPrompt regenerated =
        recallPromptService.regenerateAQuestion(
            contestResult, recallPrompt.getMcq().getNote(), recallPrompt.getMcq(), recallPrompt);
    return com.odde.donut.controllers.dto.RecallPrompt.from(regenerated);
  }

  @PostMapping("/{recallPrompt}/contest")
  @Transactional
  public QuestionContestResult contest(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt)
      throws UnexpectedNoAccessRightException {
    assertCanMutateRecallPrompt(recallPrompt);
    return recallPromptService.contest(recallPrompt);
  }

  @PostMapping("/{recallPrompt}/answer")
  @Transactional
  public AnsweredQuestion answer(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
      @Valid @RequestBody AnswerDTO answerDTO)
      throws UnexpectedNoAccessRightException {
    assertCanMutateRecallPrompt(recallPrompt);
    RecallPrompt answered =
        recallPromptService.answer(
            recallPrompt, answerDTO, testabilitySettings.getCurrentUTCTimestamp());
    return AnsweredQuestion.from(answered);
  }

  @PostMapping("/{recallPrompt}/answer-spelling")
  @Transactional
  public AnsweredQuestion answerSpelling(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt,
      @Valid @RequestBody AnswerSpellingDTO answerDTO)
      throws UnexpectedNoAccessRightException {
    assertCanMutateRecallPrompt(recallPrompt);
    MemoryTrackerService.SpellingAnswerResult result =
        memoryTrackerService.answerSpelling(
            recallPrompt,
            answerDTO,
            authorizationService.getCurrentUser(),
            testabilitySettings.getCurrentUTCTimestamp());
    return AnsweredQuestion.from(result.recallPrompt(), result.matchedNotes());
  }

  private void assertCanMutateRecallPrompt(RecallPrompt recallPrompt)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(recallPrompt.requireMemoryTracker());
  }
}
