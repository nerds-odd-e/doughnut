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
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import java.sql.Timestamp;
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
