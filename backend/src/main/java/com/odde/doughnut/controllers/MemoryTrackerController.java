package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.RecallQuestionService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memory-trackers")
class MemoryTrackerController {
  private final EntityPersister entityPersister;
  private final MemoryTrackerService memoryTrackerService;

  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;
  private final RecallQuestionService recallQuestionService;

  public MemoryTrackerController(
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      MemoryTrackerService memoryTrackerService,
      RecallQuestionService recallQuestionService) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.memoryTrackerService = memoryTrackerService;
    this.recallQuestionService = recallQuestionService;
  }

  @GetMapping("/{memoryTracker}/question")
  @Transactional
  public RecallPrompt askAQuestion(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    if (Boolean.TRUE.equals(memoryTracker.getSpelling())) {
      return memoryTrackerService.getSpellingQuestion(memoryTracker);
    }
    return recallQuestionService.generateAQuestion(memoryTracker);
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

  @PostMapping(path = "/{memoryTracker}/re-enable")
  @Transactional
  public MemoryTracker reEnable(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    memoryTracker.setRemovedFromTracking(false);
    entityPersister.save(memoryTracker);
    return memoryTracker;
  }

  @PatchMapping(path = "/{memoryTracker}/mark-as-repeated")
  @Transactional
  public MemoryTracker markAsRepeated(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @RequestParam("successful") boolean successful) {
    authorizationService.assertLoggedIn();
    memoryTrackerService.markAsRepeated(
        testabilitySettings.getCurrentUTCTimestamp(), successful, memoryTracker, null);
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

  @GetMapping("/{memoryTracker}/recall-prompts")
  public List<RecallPrompt> getRecallPrompts(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    return memoryTrackerService.getAllRecallPrompts(memoryTracker);
  }

  @DeleteMapping("/{memoryTracker}/recall-prompts/unanswered")
  @Transactional
  public void deleteUnansweredRecallPrompts(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    memoryTrackerService.deleteUnansweredRecallPrompts(memoryTracker);
  }
}
