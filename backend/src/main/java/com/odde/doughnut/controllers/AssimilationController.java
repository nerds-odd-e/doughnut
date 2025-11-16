package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AssimilationCountDTO;
import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AssimilationService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.TimestampService;
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
  private final UserModel currentUser;
  private final MemoryTrackerService memoryTrackerService;
  private final TimestampService timestampService;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public AssimilationController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings,
      TimestampService timestampService) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.timestampService = timestampService;
    this.memoryTrackerService = new MemoryTrackerService(modelFactoryService, timestampService);
  }

  @GetMapping("/assimilating")
  @Transactional(readOnly = true)
  public List<Note> assimilating(@RequestParam(value = "timezone") String timezone) {
    currentUser.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    return new AssimilationService(
            currentUser, modelFactoryService, currentUTCTimestamp, timeZone, timestampService)
        .getNotesToAssimilate()
        .toList();
  }

  @PostMapping(path = "")
  @Transactional
  public List<MemoryTracker> assimilate(@RequestBody InitialInfo initialInfo) {
    currentUser.assertLoggedIn();
    return memoryTrackerService.assimilate(
        initialInfo, currentUser.getEntity(), testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping("/count")
  @Transactional(readOnly = true)
  public AssimilationCountDTO getAssimilationCount(
      @RequestParam(value = "timezone") String timezone) {
    currentUser.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    return new AssimilationService(
            currentUser, modelFactoryService, currentUTCTimestamp, timeZone, timestampService)
        .getCounts();
  }
}
