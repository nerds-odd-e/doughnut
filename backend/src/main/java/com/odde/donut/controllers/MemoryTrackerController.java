package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.RecallHistoryItem;
import com.odde.donut.controllers.dto.RecallPromptHistoryItem;
import com.odde.donut.controllers.dto.ThresholdExceededResult;
import com.odde.donut.controllers.dto.UpdateMemoryTrackerPropertyKeyDTO;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.MemoryTrackerService;
import com.odde.donut.services.RecallQuestionService;
import com.odde.donut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.Parameter;
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

  @GetMapping("/{memoryTracker}/recall-prompt")
  @Transactional
  public com.odde.donut.controllers.dto.RecallPrompt getRecallPrompt(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    RecallPrompt recallPrompt;
    if (memoryTracker.isSpelling()) {
      recallPrompt = memoryTrackerService.getSpellingQuestion(memoryTracker);
    } else {
      recallPrompt = recallQuestionService.generateAQuestion(memoryTracker);
    }
    if (recallPrompt == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "AI failed to generate recall prompt");
    }
    return com.odde.donut.controllers.dto.RecallPrompt.from(recallPrompt);
  }

  @GetMapping("/{memoryTracker}/threshold-exceeded")
  public ThresholdExceededResult getThresholdExceeded(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    return memoryTrackerService.getThresholdExceededResult(
        memoryTracker, testabilitySettings.getCurrentUTCTimestamp());
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

  @PatchMapping(path = "/{memoryTracker}/mark-as-recalled")
  @Transactional
  public MemoryTracker markAsRecalled(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @Parameter(
              schema =
                  @Schema(
                      type = "string",
                      allowableValues = {"GOOD", "AGAIN"}))
          @RequestParam("grade")
          Grade grade) {
    authorizationService.assertLoggedIn();
    if (!grade.isJustReviewGrade()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Just review accepts only GOOD or AGAIN");
    }
    memoryTrackerService.markAsRecalled(
        testabilitySettings.getCurrentUTCTimestamp(), grade, memoryTracker, null, null);
    return memoryTracker;
  }

  @GetMapping("/recent")
  public List<MemoryTracker> getRecentMemoryTrackers() {
    authorizationService.assertLoggedIn();
    return memoryTrackerService.findLast100ByUser(authorizationService.getCurrentUser().getId());
  }

  @GetMapping("/recently-recalled")
  public List<MemoryTracker> getRecentlyRecalled() {
    authorizationService.assertLoggedIn();
    return memoryTrackerService.findLast100RecalledByUser(
        authorizationService.getCurrentUser().getId());
  }

  @GetMapping("/{memoryTracker}/recall-logs")
  public List<RecallLog> getRecallLogs(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    return memoryTrackerService.getRecallLogs(memoryTracker);
  }

  @GetMapping("/{memoryTracker}/recall-history")
  public List<RecallHistoryItem> getRecallHistory(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    return memoryTrackerService.getRecallHistory(memoryTracker);
  }

  @GetMapping("/{memoryTracker}/recall-prompts")
  public List<RecallPromptHistoryItem> getRecallPrompts(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    return memoryTrackerService.getAllRecallPrompts(memoryTracker).stream()
        .map(RecallPromptHistoryItem::from)
        .toList();
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

  @PatchMapping(path = "/{memoryTracker}/property-key")
  @Transactional
  public MemoryTracker updatePropertyKey(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @Valid @RequestBody UpdateMemoryTrackerPropertyKeyDTO dto)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    memoryTrackerService.updatePropertyKey(memoryTracker, dto.getPropertyKey());
    return memoryTracker;
  }

  @DeleteMapping("/{memoryTracker}")
  @Transactional
  public void delete(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(memoryTracker);
    memoryTrackerService.delete(memoryTracker);
  }
}
