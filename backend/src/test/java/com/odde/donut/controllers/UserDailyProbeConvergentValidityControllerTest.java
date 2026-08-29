package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.DailyProbeConvergentValidityDTO;
import com.odde.donut.controllers.dto.DailyProbeConvergentValidityDTO.PairValidity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Plan {@code 008-probe-convergent-analyses}'s internal diagnostic endpoint: same same-user-only
 * auth pattern as {@link UserRecallStatsControllerTest}, since this is per-user convergent-validity
 * data over one learner's own history, not a cross-user admin view. The correlation/gating math
 * itself is covered by {@code RecallProbeConvergentValidityTest}.
 */
class UserDailyProbeConvergentValidityControllerTest extends ControllerTestBase {
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
            () -> controller.getDailyProbeConvergentValidity("Asia/Shanghai"));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void emptyHistoryYieldsFourPairsAllWithNoDataAndNullCorrelations() {
    DailyProbeConvergentValidityDTO dto =
        controller.getDailyProbeConvergentValidity("Asia/Shanghai");

    assertThat(dto.getPairs(), hasSize(4));
    for (PairValidity pair : dto.getPairs()) {
      assertThat(pair.getPairCount(), equalTo(0));
      assertThat(pair.getRawCorrelation(), nullValue());
    }
  }
}
