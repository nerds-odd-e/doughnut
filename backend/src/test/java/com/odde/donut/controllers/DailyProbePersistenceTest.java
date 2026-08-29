package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.DailyProbe;
import com.odde.donut.entities.repositories.DailyProbeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DailyProbePersistenceTest extends ControllerTestBase {

  @Autowired DailyProbeRepository dailyProbeRepository;
  @Autowired ObjectMapper objectMapper;

  @Test
  void completedDailyProbePersistsUserSummariesAndTwentyScoredTrials() throws Exception {
    DailyProbe saved = makeMe.aDailyProbe().please();
    makeMe.entityPersister.flush();

    DailyProbe loaded = dailyProbeRepository.findById(saved.getId()).orElseThrow();

    assertThat(loaded.getUser().getId(), equalTo(saved.getUser().getId()));
    assertThat(loaded.getCompletedAt(), equalTo(saved.getCompletedAt()));
    assertThat(loaded.getSpeed(), equalTo(4.0));
    assertThat(loaded.getAccuracy(), equalTo(100));
    assertThat(loaded.getLapseCount(), equalTo(0));
    assertThat(loaded.getVariability(), equalTo(0.0));

    JsonNode trials = objectMapper.readTree(loaded.getTrialsJson());
    assertThat(trials.isArray(), is(true));
    assertThat(trials.size(), equalTo(20));
    assertThat(trials.get(0).get("stimulus").asText(), equalTo("left"));
    assertThat(trials.get(0).has("correct"), is(true));
  }
}
