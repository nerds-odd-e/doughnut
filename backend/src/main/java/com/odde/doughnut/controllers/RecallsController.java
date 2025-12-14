package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.RecallResult;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.RecallService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.TimezoneUtils;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;
  private final RecallService recallService;

  @Autowired
  public RecallsController(
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      RecallService recallService) {
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.recallService = recallService;
  }

  @GetMapping(value = {"/recalling"})
  @Transactional
  public DueMemoryTrackers recalling(
      @RequestParam(value = "timezone") String timezone,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    authorizationService.assertLoggedIn();
    ZoneId timeZone = TimezoneUtils.parseTimezone(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return recallService.getDueMemoryTrackers(
        authorizationService.getCurrentUser(),
        currentUTCTimestamp,
        timeZone,
        dueInDays == null ? 0 : dueInDays);
  }

  @GetMapping(value = {"/previously-answered"})
  @Transactional
  public List<RecallResult> previouslyAnswered(@RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn();
    ZoneId timeZone = TimezoneUtils.parseTimezone(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return recallService.getPreviouslyAnsweredRecallPrompts(
        authorizationService.getCurrentUser(), currentUTCTimestamp, timeZone);
  }
}
