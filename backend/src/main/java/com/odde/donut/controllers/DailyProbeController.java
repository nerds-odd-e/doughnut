package com.odde.donut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.DailyProbeRequestDTO;
import com.odde.donut.controllers.dto.DailyProbeTodayDTO;
import com.odde.donut.entities.DailyProbe;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.DailyProbeRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.utils.TimestampOperations;
import com.odde.donut.utils.TimezoneUtils;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/daily-probes")
class DailyProbeController {
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final AuthorizationService authorizationService;
  private final ObjectMapper objectMapper;
  private final DailyProbeRepository dailyProbeRepository;

  public DailyProbeController(
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      ObjectMapper objectMapper,
      DailyProbeRepository dailyProbeRepository) {
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.objectMapper = objectMapper;
    this.dailyProbeRepository = dailyProbeRepository;
  }

  @GetMapping("/today")
  @Transactional(readOnly = true)
  public DailyProbeTodayDTO getDailyProbeToday(@RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    ZoneId zoneId = TimezoneUtils.parseTimezone(timezone);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    boolean completed =
        dailyProbeRepository.existsByUserAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            user,
            TimestampOperations.getStartOfDay(now, zoneId),
            TimestampOperations.getStartOfNextDay(now, zoneId));
    return new DailyProbeTodayDTO(completed);
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
