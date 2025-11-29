package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.SelfEvaluation;
import com.odde.doughnut.controllers.dto.SpellingQuestion;
import com.odde.doughnut.controllers.dto.SpellingResultDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/memory-trackers")
class MemoryTrackerController {
  private final EntityPersister entityPersister;
  private final MemoryTrackerService memoryTrackerService;

  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;

  public MemoryTrackerController(
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      MemoryTrackerService memoryTrackerService) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.memoryTrackerService = memoryTrackerService;
  }

  @GetMapping("/{memoryTracker}/spelling-question")
  public SpellingQuestion getSpellingQuestion(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    return new SpellingQuestion(
        memoryTracker.getNote().getClozeDescription().clozeDetails(),
        memoryTracker.getNote().getNotebook());
  }

  @GetMapping("/{memoryTracker}")
  public MemoryTracker showMemoryTracker(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    return memoryTracker;
  }

  @PostMapping(path = "/{memoryTracker}/remove")
  @Transactional
  public MemoryTracker removeFromRepeating(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker) {
    memoryTracker.setRemovedFromTracking(true);
    memoryTracker.setLastRecalledAt(testabilitySettings.getCurrentUTCTimestamp());
    entityPersister.save(memoryTracker);
    return memoryTracker;
  }

  @PostMapping(path = "/{memoryTracker}/self-evaluate")
  @Transactional
  public MemoryTracker selfEvaluate(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @RequestBody SelfEvaluation selfEvaluation) {
    authorizationService.assertLoggedIn();
    if (memoryTracker == null || memoryTracker.getId() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The memory tracker does not exist.");
    }
    memoryTrackerService.updateForgettingCurve(memoryTracker, selfEvaluation.adjustment);
    return memoryTracker;
  }

  @PatchMapping(path = "/{memoryTracker}/mark-as-repeated")
  @Transactional
  public MemoryTracker markAsRepeated(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @RequestParam("successful") boolean successful) {
    authorizationService.assertLoggedIn();
    memoryTrackerService.markAsRepeated(
        testabilitySettings.getCurrentUTCTimestamp(), successful, memoryTracker);
    return memoryTracker;
  }

  @GetMapping("/recent")
  public List<MemoryTracker> getRecentMemoryTrackers() {
    authorizationService.assertLoggedIn();
    return memoryTrackerService.findLast100ByUser(authorizationService.getCurrentUser().getId());
  }

  @GetMapping("/recently-reviewed")
  public List<MemoryTracker> getRecentlyReviewed() {
    authorizationService.assertLoggedIn();
    return memoryTrackerService.findLast100ReviewedByUser(
        authorizationService.getCurrentUser().getId());
  }

  @PostMapping("/{memoryTracker}/answer-spelling")
  @Transactional
  public SpellingResultDTO answerSpelling(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @Valid @RequestBody AnswerSpellingDTO answerDTO) {
    authorizationService.assertLoggedIn();
    return memoryTrackerService.answerSpelling(
        memoryTracker,
        answerDTO,
        authorizationService.getCurrentUser(),
        testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping("/{memoryTracker}/recall-prompts")
  public List<RecallPrompt> getRecallPrompts(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    return memoryTrackerService.getAllRecallPrompts(memoryTracker);
  }
}
