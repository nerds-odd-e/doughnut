package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.DailyProbeRequestDTO;
import com.odde.donut.controllers.dto.DailyProbeTodayDTO;
import com.odde.donut.controllers.dto.DailyProbeTrialDTO;
import com.odde.donut.entities.DailyProbe;
import com.odde.donut.entities.repositories.DailyProbeRepository;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DailyProbeControllerTest extends ControllerTestBase {
  @Autowired DailyProbeController controller;
  @Autowired DailyProbeRepository dailyProbeRepository;

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

    assertThat(loaded.getTrials(), hasSize(20));
    DailyProbeTrialDTO trial = loaded.getTrials().get(0);
    assertThat(
        List.of(trial.stimulus, trial.response, trial.rtMs, trial.correct),
        contains("left", "left", 250, true));
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

  @Test
  void getTodayCompletedWhenCompletedAtFallsOnShanghaiLocalDayThatDiffersFromUtcDate() {
    Timestamp now = makeMe.aTimestamp().of(1, 4).fromShanghai().please();
    testabilitySettings.timeTravelTo(now);
    makeMe.aDailyProbe().by(currentUser.getUser()).please();

    DailyProbeTodayDTO today = controller.getDailyProbeToday("Asia/Shanghai");

    assertThat(today.completed(), is(true));
  }

  @Test
  void getTodayNotCompletedAfterShanghaiMidnightWhenCompletedBeforeMidnight() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 23).fromShanghai().please());
    makeMe.aDailyProbe().by(currentUser.getUser()).please();

    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 0).fromShanghai().please());

    assertThat(controller.getDailyProbeToday("Asia/Shanghai").completed(), is(false));
  }

  @Test
  void getTodayNotCompletedWithoutARow() {
    assertThat(controller.getDailyProbeToday("Asia/Shanghai").completed(), is(false));
  }

  @Test
  void getTodayIgnoresAnotherUsersCompletedRow() {
    Timestamp now = makeMe.aTimestamp().of(1, 4).fromShanghai().please();
    testabilitySettings.timeTravelTo(now);
    makeMe.aDailyProbe().please();

    assertThat(controller.getDailyProbeToday("Asia/Shanghai").completed(), is(false));
  }

  @Test
  void getTodayNotLoggedIn() {
    currentUser.setUser(null);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.getDailyProbeToday("Asia/Shanghai"));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
  }
}
