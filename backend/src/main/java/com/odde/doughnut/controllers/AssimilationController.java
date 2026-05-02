package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.AssimilationService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.NoteRealmService;
import com.odde.doughnut.services.SubscriptionService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.TimezoneUtils;
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

  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;
  private final NoteRealmService noteRealmService;

  @Autowired
  public AssimilationController(
      MemoryTrackerService memoryTrackerService,
      SubscriptionService subscriptionService,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      UserService userService,
      NoteRealmService noteRealmService) {
    this.memoryTrackerService = memoryTrackerService;
    this.subscriptionService = subscriptionService;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.userService = userService;
    this.noteRealmService = noteRealmService;
  }

  @GetMapping("/assimilating")
  @Transactional(readOnly = true)
  public List<NoteRealm> assimilating(@RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    ZoneId timeZone = TimezoneUtils.parseTimezone(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    return new AssimilationService(
            user, userService, subscriptionService, currentUTCTimestamp, timeZone)
        .getNotesToAssimilate()
        .map(note -> noteRealmService.build(note, user))
        .toList();
  }

  @PostMapping(path = "")
  @Transactional
  public List<MemoryTracker> assimilate(@RequestBody AssimilationRequestDTO request) {
    authorizationService.assertLoggedIn();
    return memoryTrackerService.assimilate(
        request,
        authorizationService.getCurrentUser(),
        testabilitySettings.getCurrentUTCTimestamp());
  }
}
