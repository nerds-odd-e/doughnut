package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.DailyProbeRequestDTO;
import com.odde.donut.entities.DailyProbe;
import com.odde.donut.entities.repositories.DailyProbeRepository;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DailyProbeControllerTest extends ControllerTestBase {
  @Autowired DailyProbeController controller;
  @Autowired DailyProbeRepository dailyProbeRepository;
  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void completingPersistsOwnerTwentyTrialsSummariesAndCompletedAt() throws Exception {
    Timestamp now = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    testabilitySettings.timeTravelTo(now);

    DailyProbe saved = controller.createDailyProbe(makeMe.aDailyProbe().createRequest());
    makeMe.entityPersister.flush();

    DailyProbe loaded = dailyProbeRepository.findById(saved.getId()).orElseThrow();
    assertThat(loaded.getUser().getId(), equalTo(currentUser.getUser().getId()));
    assertThat(loaded.getCompletedAt(), equalTo(now));
    assertThat(loaded.getSpeed(), equalTo(4.0));
    assertThat(loaded.getAccuracy(), equalTo(100));
    assertThat(loaded.getLapseCount(), equalTo(0));
    assertThat(loaded.getVariability(), equalTo(0.0));

    JsonNode trials = objectMapper.readTree(loaded.getTrialsJson());
    assertThat(trials.isArray(), is(true));
    assertThat(trials.size(), equalTo(20));
    assertThat(trials.get(0).get("stimulus").asText(), equalTo("left"));
    assertThat(trials.get(0).get("response").asText(), equalTo("left"));
    assertThat(trials.get(0).get("rtMs").asInt(), equalTo(250));
    assertThat(trials.get(0).get("correct").asBoolean(), is(true));
  }

  @Test
  void notLoggedIn() {
    currentUser.setUser(null);
    assertThrows(
        ResponseStatusException.class,
        () -> controller.createDailyProbe(makeMe.aDailyProbe().createRequest()));
  }

  @Test
  void rejectsWhenNotTwentyTrials() {
    DailyProbeRequestDTO request = makeMe.aDailyProbe().createRequest();
    request.trials.removeLast();

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.createDailyProbe(request));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(dailyProbeRepository.count(), equalTo(0L));
  }
}
