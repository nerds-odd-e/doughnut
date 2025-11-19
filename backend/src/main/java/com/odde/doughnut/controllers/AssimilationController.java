package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.AssimilationCountDTO;
import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AssimilationService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.SubscriptionService;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/assimilation")
class AssimilationController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUser currentUser;
  private final MemoryTrackerService memoryTrackerService;
  private final SubscriptionService subscriptionService;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;

  public AssimilationController(
      ModelFactoryService modelFactoryService,
      CurrentUser currentUser,
      SubscriptionService subscriptionService,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.subscriptionService = subscriptionService;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.memoryTrackerService = new MemoryTrackerService(modelFactoryService);
  }

  @GetMapping("/assimilating")
  @Transactional(readOnly = true)
  public List<Note> assimilating(@RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    return new AssimilationService(
            currentUser.getUserModel(),
            modelFactoryService,
            subscriptionService,
            currentUTCTimestamp,
            timeZone)
        .getNotesToAssimilate()
        .toList();
  }

  @PostMapping(path = "")
  @Transactional
  public List<MemoryTracker> assimilate(@RequestBody InitialInfo initialInfo) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    return memoryTrackerService.assimilate(
        initialInfo, currentUser.getUser(), testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping("/count")
  @Transactional(readOnly = true)
  public AssimilationCountDTO getAssimilationCount(
      @RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    return new AssimilationService(
            currentUser.getUserModel(),
            modelFactoryService,
            subscriptionService,
            currentUTCTimestamp,
            timeZone)
        .getCounts();
  }
}
