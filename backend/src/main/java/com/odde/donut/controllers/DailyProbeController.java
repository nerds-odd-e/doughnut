package com.odde.donut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.DailyProbeRequestDTO;
import com.odde.donut.entities.DailyProbe;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.testability.TestabilitySettings;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/daily-probes")
class DailyProbeController {
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final AuthorizationService authorizationService;
  private final ObjectMapper objectMapper;

  public DailyProbeController(
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      ObjectMapper objectMapper) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.objectMapper = objectMapper;
  }

  @PostMapping(path = "")
  @Transactional
  public DailyProbe createDailyProbe(@RequestBody DailyProbeRequestDTO request)
      throws JsonProcessingException {
    authorizationService.assertLoggedIn();
    if (request.trials == null || request.trials.size() != 20) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Daily probe requires exactly 20 scored trials");
    }
    DailyProbe probe = new DailyProbe();
    probe.setUser(authorizationService.getCurrentUser());
    probe.setCompletedAt(testabilitySettings.getCurrentUTCTimestamp());
    probe.setSpeed(request.speed);
    probe.setAccuracy(request.accuracy);
    probe.setLapseCount(request.lapseCount);
    probe.setVariability(request.variability);
    probe.setTrialsJson(objectMapper.writeValueAsString(request.trials));
    return entityPersister.save(probe);
  }
}
