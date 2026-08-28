package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.RecallSplitHalfReliabilityDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Slice 21.4's internal diagnostic endpoint: same same-user-only auth pattern as {@link
 * UserRecallStatsControllerTest}, since this is per-user reliability data over one learner's own
 * history, not a cross-user admin view. The correlation math itself is covered by {@code
 * RecallSplitHalfReliabilityTest}.
 */
class UserRecallSplitHalfReliabilityControllerTest extends ControllerTestBase {
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
            ResponseStatusException.class,
            () -> controller.getRecallSplitHalfReliability("Asia/Shanghai"));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void emptyHistoryYieldsNoPairsAndNullCorrelations() {
    RecallSplitHalfReliabilityDTO dto = controller.getRecallSplitHalfReliability("Asia/Shanghai");
    assertThat(dto.getPairCount(), equalTo(0));
    assertThat(dto.getRawCorrelation(), nullValue());
    assertThat(dto.getSpearmanBrownCorrelation(), nullValue());
  }
}
