package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AssimilationNextDTO;
import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.AssimilationService;
import com.odde.doughnut.services.AssimilationUnit;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.SubscriptionService;
import com.odde.doughnut.services.UnassimilatedPropertyService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.TimezoneUtils;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
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
  private final UnassimilatedPropertyService unassimilatedPropertyService;

  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;

  @Autowired
  public AssimilationController(
      MemoryTrackerService memoryTrackerService,
      SubscriptionService subscriptionService,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      UserService userService,
      UnassimilatedPropertyService unassimilatedPropertyService) {
    this.memoryTrackerService = memoryTrackerService;
    this.subscriptionService = subscriptionService;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.userService = userService;
    this.unassimilatedPropertyService = unassimilatedPropertyService;
  }

  @GetMapping("/next")
  @Transactional(readOnly = true)
  public AssimilationNextDTO next(@RequestParam(value = "timezone") String timezone) {
    var scope = assimilationScope(timezone);
    Optional<AssimilationUnit> nextUnit = scope.service().getNextAssimilationUnit();
    return new AssimilationNextDTO(
        nextUnit.map(unit -> unit.note().getId()).orElse(null),
        nextUnit
            .filter(AssimilationUnit::isPropertyLevel)
            .map(AssimilationUnit::propertyKey)
            .orElse(null),
        scope.service().getCounts());
  }

  private record AssimilationScope(User user, AssimilationService service) {}

  private AssimilationScope assimilationScope(String timezone) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    ZoneId timeZone = TimezoneUtils.parseTimezone(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return new AssimilationScope(
        user,
        new AssimilationService(
            user,
            userService,
            subscriptionService,
            unassimilatedPropertyService,
            currentUTCTimestamp,
            timeZone));
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
