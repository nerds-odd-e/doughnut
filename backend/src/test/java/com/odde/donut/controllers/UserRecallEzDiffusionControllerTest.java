package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.RecallEzDiffusionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Plan {@code 008-probe-convergent-analyses} slice 3's internal diagnostic endpoint: same
 * same-user-only auth pattern as {@link UserDailyProbeConvergentValidityControllerTest}. Trial
 * selection/pooling and the EZ-diffusion algebra are covered by {@code RecallEzDiffusionTest} and
 * {@code EzDiffusionTest}.
 */
class UserRecallEzDiffusionControllerTest extends ControllerTestBase {
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
            ResponseStatusException.class, () -> controller.getRecallEzDiffusion("Asia/Shanghai"));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void emptyHistoryYieldsZeroTrialsZeroMorningsAndNullFit() {
    RecallEzDiffusionDTO dto = controller.getRecallEzDiffusion("Asia/Shanghai");

    assertThat(dto.getTrialCount(), equalTo(0));
    assertThat(dto.getMorningCount(), equalTo(0));
    assertThat(dto.getDriftRate(), nullValue());
    assertThat(dto.getBoundarySeparation(), nullValue());
    assertThat(dto.getNondecisionTimeMs(), nullValue());
  }
}
