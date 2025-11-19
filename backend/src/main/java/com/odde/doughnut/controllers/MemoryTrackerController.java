package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.SelfEvaluation;
import com.odde.doughnut.controllers.dto.SpellingQuestion;
import com.odde.doughnut.controllers.dto.SpellingResultDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/memory-trackers")
class MemoryTrackerController {
  private final ModelFactoryService modelFactoryService;
  private final MemoryTrackerService memoryTrackerService;
  private final UserService userService;
  private final AuthorizationService authorizationService;
  private User currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public MemoryTrackerController(
      ModelFactoryService modelFactoryService,
      @Qualifier("currentUserEntity") User currentUser,
      UserService userService,
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.userService = userService;
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.memoryTrackerService = new MemoryTrackerService(modelFactoryService);
  }

  @GetMapping("/{memoryTracker}/spelling-question")
  public SpellingQuestion getSpellingQuestion(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    userService.assertLoggedIn(currentUser);
    authorizationService.assertReadAuthorization(currentUser, memoryTracker);
    return new SpellingQuestion(
        memoryTracker.getNote().getClozeDescription().clozeDetails(),
        memoryTracker.getNote().getNotebook());
  }

  @GetMapping("/{memoryTracker}")
  public MemoryTracker showMemoryTracker(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker)
      throws UnexpectedNoAccessRightException {
    userService.assertLoggedIn(currentUser);
    authorizationService.assertReadAuthorization(currentUser, memoryTracker);
    return memoryTracker;
  }

  @PostMapping(path = "/{memoryTracker}/remove")
  @Transactional
  public MemoryTracker removeFromRepeating(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker) {
    memoryTracker.setRemovedFromTracking(true);
    memoryTracker.setLastRecalledAt(testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.save(memoryTracker);
    return memoryTracker;
  }

  @PostMapping(path = "/{memoryTracker}/self-evaluate")
  @Transactional
  public MemoryTracker selfEvaluate(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @RequestBody SelfEvaluation selfEvaluation) {
    userService.assertLoggedIn(currentUser);
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
    userService.assertLoggedIn(currentUser);
    memoryTrackerService.markAsRepeated(
        testabilitySettings.getCurrentUTCTimestamp(), successful, memoryTracker);
    return memoryTracker;
  }

  @GetMapping("/recent")
  public List<MemoryTracker> getRecentMemoryTrackers() {
    userService.assertLoggedIn(currentUser);
    return modelFactoryService.memoryTrackerRepository.findLast100ByUser(currentUser.getId());
  }

  @GetMapping("/recently-reviewed")
  public List<MemoryTracker> getRecentlyReviewed() {
    userService.assertLoggedIn(currentUser);
    return modelFactoryService.memoryTrackerRepository.findLast100ReviewedByUser(
        currentUser.getId());
  }

  @PostMapping("/{memoryTracker}/answer-spelling")
  @Transactional
  public SpellingResultDTO answerSpelling(
      @PathVariable("memoryTracker") @Schema(type = "integer") MemoryTracker memoryTracker,
      @Valid @RequestBody AnswerSpellingDTO answerDTO) {
    userService.assertLoggedIn(currentUser);
    return memoryTrackerService.answerSpelling(
        memoryTracker, answerDTO, currentUser, testabilitySettings.getCurrentUTCTimestamp());
  }
}
