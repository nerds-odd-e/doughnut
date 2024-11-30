package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.SelfEvaluation;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/memory-trackers")
class RestReviewPointController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestReviewPointController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/{memoryTracker}")
  public MemoryTracker show(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(memoryTracker);
    return memoryTracker;
  }

  @PostMapping(path = "/{memoryTracker}/remove")
  @Transactional
  public MemoryTracker removeFromRepeating(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker) {
    memoryTracker.setRemovedFromReview(true);
    memoryTracker.setLastReviewedAt(testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.save(memoryTracker);
    return memoryTracker;
  }

  @PostMapping(path = "/{memoryTracker}/self-evaluate")
  @Transactional
  public MemoryTracker selfEvaluate(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @RequestBody SelfEvaluation selfEvaluation) {
    currentUser.assertLoggedIn();
    if (memoryTracker == null || memoryTracker.getId() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The memory tracker does not exist.");
    }
    modelFactoryService
        .toReviewPointModel(memoryTracker)
        .updateForgettingCurve(selfEvaluation.adjustment);
    return memoryTracker;
  }

  @PatchMapping(path = "/{memoryTracker}/mark-as-repeated")
  @Transactional
  public MemoryTracker markAsRepeated(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @RequestParam("successful") boolean successful) {
    currentUser.assertLoggedIn();
    modelFactoryService
        .toReviewPointModel(memoryTracker)
        .markAsRepeated(testabilitySettings.getCurrentUTCTimestamp(), successful);
    return memoryTracker;
  }

  @GetMapping("/recent")
  public List<MemoryTracker> getRecentReviewPoints() {
    currentUser.assertLoggedIn();
    return modelFactoryService.memoryTrackerRepository.findLast100ByUser(
        currentUser.getEntity().getId());
  }

  @GetMapping("/recently-reviewed")
  public List<MemoryTracker> getRecentlyReviewedPoints() {
    currentUser.assertLoggedIn();
    return modelFactoryService.memoryTrackerRepository.findLast100ReviewedByUser(
        currentUser.getEntity().getId());
  }
}
