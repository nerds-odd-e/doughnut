package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import com.odde.donut.controllers.dto.RecallStatsDTO.DailyProbeDay;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

class UserRecallStatsControllerTest extends ControllerTestBase {
  @Autowired UserController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void requiresLogin() {
    currentUser.setUser(null);
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.getRecallStats("Asia/Shanghai"));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void emptyStatsShape() {
    RecallStatsDTO dto = controller.getRecallStats("Asia/Shanghai");
    assertThat(dto.getCalendar(), hasSize(365));
    assertThat(dto.getCalendar(), everyItem(hasProperty("count", equalTo(0))));
    assertThat(dto.getRetentionTrend(), hasSize(90));
    assertThat(dto.getRetentionTrend(), everyItem(hasProperty("retentionPct", nullValue())));
    assertThat(dto.getTotals(), notNullValue());
    assertThat(dto.getTotals().getRetentionPct365(), nullValue());
    assertThat(dto.getPace(), notNullValue());
    assertThat(dto.getPace().getSampleSize(), equalTo(0));
    assertThat(dto.getPace().getPctVsUsual(), nullValue());
    assertThat(dto.getDailyProbe(), hasSize(0));
  }

  @Test
  void completedDailyProbeDaysAreASparseOldestFirstSeries() {
    Timestamp older = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    Timestamp newer = makeMe.aTimestamp().of(2, 8).fromShanghai().please();
    makeMe
        .aDailyProbe()
        .by(currentUser.getUser())
        .completedAt(newer)
        .speed(4.0)
        .lapseCount(0)
        .variability(0.0)
        .please();
    makeMe
        .aDailyProbe()
        .by(currentUser.getUser())
        .completedAt(older)
        .speed(3.0)
        .lapseCount(2)
        .variability(1.41)
        .please();

    List<DailyProbeDay> series = controller.getRecallStats("Asia/Shanghai").getDailyProbe();

    assertThat(series, hasSize(2));
    assertThat(series.get(0).getDate(), equalTo("1989-01-02"));
    assertThat(series.get(0).getSpeed(), equalTo(3.0));
    assertThat(series.get(0).getLapses(), equalTo(2));
    assertThat(series.get(0).getVariability(), equalTo(1.41));
    assertThat(series.get(1).getDate(), equalTo("1989-01-03"));
    assertThat(series.get(1).getSpeed(), equalTo(4.0));
    assertThat(series.get(1).getLapses(), equalTo(0));
    assertThat(series.get(1).getVariability(), equalTo(0.0));
  }

  @Test
  void dailyProbeOmitsOtherUsersRows() {
    makeMe.aDailyProbe().please();

    assertThat(controller.getRecallStats("UTC").getDailyProbe(), hasSize(0));
  }

  @Test
  void dailyProbeGroupsCompletedAtByRequestTimezone() {
    Timestamp completedAt = makeMe.aTimestamp().of(1, 4).fromShanghai().please();
    makeMe.aDailyProbe().by(currentUser.getUser()).completedAt(completedAt).please();

    assertThat(
        controller.getRecallStats("Asia/Shanghai").getDailyProbe().get(0).getDate(),
        equalTo("1989-01-02"));
    assertThat(
        controller.getRecallStats("UTC").getDailyProbe().get(0).getDate(), equalTo("1989-01-01"));
  }

  @Test
  void reviewsTodayUsesTestabilityClockAndUserZoneTodayBoundary() {
    Timestamp now = makeMe.aTimestamp().of(0, 0).please();
    testabilitySettings.timeTravelTo(now);

    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    MemoryTracker mt = makeMe.aMemoryTrackerFor(note).please();
    Timestamp todayAnswer =
        Timestamp.from(
            java.time.ZonedDateTime.of(1988, 12, 31, 17, 0, 0, 0, java.time.ZoneId.of("UTC"))
                .toInstant());
    makeMe
        .aRecallPrompt()
        .withMcqForNote(note)
        .forMemoryTracker(mt)
        .answerChoiceIndex(0)
        .answerTimestamp(todayAnswer)
        .please();

    assertThat(
        controller.getRecallStats("Asia/Shanghai").getTotals().getReviewsToday(), equalTo(1));
  }

  @Test
  void scopedToCurrentUserExcludesOtherUsersPrompts() {
    Timestamp now = makeMe.aTimestamp().of(10, 12).please();
    testabilitySettings.timeTravelTo(now);

    Note myNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    MemoryTracker myMt = makeMe.aMemoryTrackerFor(myNote).please();
    Timestamp myAnswer = makeMe.aTimestamp().of(9, 10).please();
    makeMe
        .aRecallPrompt()
        .withMcqForNote(myNote)
        .forMemoryTracker(myMt)
        .answerChoiceIndex(0)
        .answerTimestamp(myAnswer)
        .please();

    User other = makeMe.aUser().please();
    Note otherNote = makeMe.aNote().notebookOwnedBy(other).please();
    MemoryTracker otherMt = makeMe.aMemoryTrackerFor(otherNote).by(other).please();
    Timestamp otherAnswer = makeMe.aTimestamp().of(9, 10).please();
    makeMe
        .aRecallPrompt()
        .withMcqForNote(otherNote)
        .forMemoryTracker(otherMt)
        .answerChoiceIndex(0)
        .answerTimestamp(otherAnswer)
        .please();

    assertThat(controller.getRecallStats("UTC").getTotals().getTotalReviews365(), equalTo(1));
  }
}
