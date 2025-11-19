package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.RecallStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.services.RecallService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Qualifier;
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
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final UserService userService;
  private final User currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RecallsController(
      MemoryTrackerRepository memoryTrackerRepository,
      UserService userService,
      @Qualifier("currentUserEntity") User currentUser,
      TestabilitySettings testabilitySettings) {
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.userService = userService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/overview")
  @Transactional(readOnly = true)
  public RecallStatus overview(@RequestParam(value = "timezone") String timezone) {
    userService.assertLoggedIn(currentUser);
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return new RecallService(
            currentUser, currentUTCTimestamp, timeZone, memoryTrackerRepository, userService)
        .getRecallStatus();
  }

  @GetMapping(value = {"/recalling"})
  @Transactional
  public DueMemoryTrackers recalling(
      @RequestParam(value = "timezone") String timezone,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    userService.assertLoggedIn(currentUser);
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return new RecallService(
            currentUser, currentUTCTimestamp, timeZone, memoryTrackerRepository, userService)
        .getDueMemoryTrackers(dueInDays == null ? 0 : dueInDays);
  }
}
