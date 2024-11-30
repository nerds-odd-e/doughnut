package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.DueReviewPoints;
import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.controllers.dto.ReviewStatus;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.MemoryTrackerModel;
import com.odde.doughnut.models.Reviewing;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import java.time.ZoneId;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/reviews")
class RestReviewsController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestReviewsController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/overview")
  @Transactional(readOnly = true)
  public ReviewStatus overview(@RequestParam(value = "timezone") String timezone) {
    currentUser.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    return currentUser
        .createReviewing(testabilitySettings.getCurrentUTCTimestamp(), timeZone)
        .getReviewStatus();
  }

  @GetMapping("/initial")
  @Transactional(readOnly = true)
  public List<Note> initialReview(@RequestParam(value = "timezone") String timezone) {
    currentUser.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    Reviewing reviewing =
        currentUser.createReviewing(testabilitySettings.getCurrentUTCTimestamp(), timeZone);

    return reviewing.getDueInitialReviewPoints().toList();
  }

  @PostMapping(path = "")
  @Transactional
  public MemoryTracker create(@RequestBody InitialInfo initialInfo) {
    currentUser.assertLoggedIn();
    MemoryTracker memoryTracker =
        MemoryTracker.buildReviewPointForNote(
            modelFactoryService.entityManager.find(Note.class, initialInfo.noteId));
    memoryTracker.setRemovedFromReview(initialInfo.skipReview);

    MemoryTrackerModel memoryTrackerModel = modelFactoryService.toReviewPointModel(memoryTracker);
    memoryTrackerModel.initialReview(
        testabilitySettings.getCurrentUTCTimestamp(), currentUser.getEntity());
    return memoryTrackerModel.getEntity();
  }

  @GetMapping(value = {"/repeat"})
  @Transactional
  public DueReviewPoints repeatReview(
      @RequestParam(value = "timezone") String timezone,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    currentUser.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    Reviewing reviewing =
        currentUser.createReviewing(testabilitySettings.getCurrentUTCTimestamp(), timeZone);
    return reviewing.getDueReviewPoints(dueInDays == null ? 0 : dueInDays);
  }
}
