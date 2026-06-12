package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AssimilationNextDTO;
import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.AssimilationService;
import com.odde.doughnut.services.AssimilationServiceFactory;
import com.odde.doughnut.services.AssimilationUnit;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.MemoryTrackerService;
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
  private final AssimilationServiceFactory assimilationServiceFactory;
  private final TestabilitySettings testabilitySettings;
  private final AuthorizationService authorizationService;

  @Autowired
  public AssimilationController(
      MemoryTrackerService memoryTrackerService,
      AssimilationServiceFactory assimilationServiceFactory,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService) {
    this.memoryTrackerService = memoryTrackerService;
    this.assimilationServiceFactory = assimilationServiceFactory;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
  }

  @GetMapping("/next")
  @Transactional(readOnly = true)
  public AssimilationNextDTO next(@RequestParam(value = "timezone") String timezone) {
    AssimilationService service = assimilationService(timezone);
    Optional<AssimilationUnit> nextUnit = service.getNextAssimilationUnit();
    return AssimilationNextDTO.from(nextUnit, service.getCounts());
  }

  private AssimilationService assimilationService(String timezone) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    ZoneId timeZone = TimezoneUtils.parseTimezone(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return assimilationServiceFactory.create(user, currentUTCTimestamp, timeZone);
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
