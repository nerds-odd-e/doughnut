package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.RecallStatus;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.RecallService;
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
  private final ModelFactoryService modelFactoryService;
  private final CurrentUser currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;

  public RecallsController(
      ModelFactoryService modelFactoryService,
      CurrentUser currentUser,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
  }

  @GetMapping("/overview")
  @Transactional(readOnly = true)
  public RecallStatus overview(@RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return new RecallService(
            currentUser.getUserModel(), currentUTCTimestamp, timeZone, modelFactoryService)
        .getRecallStatus();
  }

  @GetMapping(value = {"/recalling"})
  @Transactional
  public DueMemoryTrackers recalling(
      @RequestParam(value = "timezone") String timezone,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return new RecallService(
            currentUser.getUserModel(), currentUTCTimestamp, timeZone, modelFactoryService)
        .getDueMemoryTrackers(dueInDays == null ? 0 : dueInDays);
  }
}
