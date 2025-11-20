package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AssimilationCountDTO;
import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AssimilationService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.SubscriptionService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/assimilation")
class AssimilationController {
  private final MemoryTrackerService memoryTrackerService;
  private final SubscriptionService subscriptionService;
  private final UserService userService;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;

  @Autowired
  public AssimilationController(
      EntityPersister entityPersister,
      SubscriptionService subscriptionService,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      UserService userService) {
    this.subscriptionService = subscriptionService;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.userService = userService;
    this.memoryTrackerService = new MemoryTrackerService(entityPersister, userService);
  }

  @GetMapping("/assimilating")
  @Transactional(readOnly = true)
  public List<Note> assimilating(@RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    return new AssimilationService(
            user, userService, subscriptionService, currentUTCTimestamp, timeZone)
        .getNotesToAssimilate()
        .toList();
  }

  @PostMapping(path = "")
  @Transactional
  public List<MemoryTracker> assimilate(@RequestBody InitialInfo initialInfo) {
    authorizationService.assertLoggedIn();
    return memoryTrackerService.assimilate(
        initialInfo,
        authorizationService.getCurrentUser(),
        testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping("/count")
  @Transactional(readOnly = true)
  public AssimilationCountDTO getAssimilationCount(
      @RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    return new AssimilationService(
            user, userService, subscriptionService, currentUTCTimestamp, timeZone)
        .getCounts();
  }
}
