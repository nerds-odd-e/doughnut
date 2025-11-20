package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.RecallStatus;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.RecallService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/recalls")
class RecallsController {
  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;
  private final UserService userService;
  private final MemoryTrackerRepository memoryTrackerRepository;

  public RecallsController(
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      UserService userService,
      MemoryTrackerRepository memoryTrackerRepository) {
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.userService = userService;
    this.memoryTrackerRepository = memoryTrackerRepository;
  }

  @GetMapping("/overview")
  @Transactional(readOnly = true)
  public RecallStatus overview(@RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return new RecallService(
            authorizationService.getCurrentUser(),
            userService,
            currentUTCTimestamp,
            timeZone,
            memoryTrackerRepository)
        .getRecallStatus();
  }

  @GetMapping(value = {"/recalling"})
  @Transactional
  public DueMemoryTrackers recalling(
      @RequestParam(value = "timezone") String timezone,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    authorizationService.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return new RecallService(
            authorizationService.getCurrentUser(),
            userService,
            currentUTCTimestamp,
            timeZone,
            memoryTrackerRepository)
        .getDueMemoryTrackers(dueInDays == null ? 0 : dueInDays);
  }
}
