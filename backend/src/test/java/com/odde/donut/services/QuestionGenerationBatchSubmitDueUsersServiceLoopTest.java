package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.donut.entities.User;
import com.odde.donut.services.QuestionGenerationBatchUserSubmissionTx.DueUserSubmissionOutcome;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestionGenerationBatchSubmitDueUsersServiceLoopTest {

  private final Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 8, 3, 16, 45));

  QuestionGenerationBatchPlanningService planningService;
  QuestionGenerationBatchUserSubmissionTx userSubmissionTx;
  QuestionGenerationBatchSubmitDueUsersService service;

  @BeforeEach
  void setUp() {
    planningService = mock(QuestionGenerationBatchPlanningService.class);
    userSubmissionTx = mock(QuestionGenerationBatchUserSubmissionTx.class);
    service = new QuestionGenerationBatchSubmitDueUsersService(planningService, userSubmissionTx);
  }

  @Test
  void processesAllUsersThenThrowsFirstFailure() {
    User failingUser = new User();
    User successfulUser = new User();
    when(planningService.findUsersEligibleForBatchSubmission(cronTime))
        .thenReturn(List.of(failingUser, successfulUser));
    when(userSubmissionTx.processDueUser(argThat(u -> u == failingUser), eq(cronTime)))
        .thenThrow(new RuntimeException("upload failed"));
    when(userSubmissionTx.processDueUser(argThat(u -> u == successfulUser), eq(cronTime)))
        .thenReturn(DueUserSubmissionOutcome.submitted(2, 20, "batch-ok"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> service.submitDueUsers(cronTime));

    assertThat(thrown.getMessage(), containsString("upload failed"));
    assertThat(thrown.getCause().getMessage(), equalTo("upload failed"));
    verify(userSubmissionTx).processDueUser(argThat(u -> u == failingUser), eq(cronTime));
    verify(userSubmissionTx).processDueUser(argThat(u -> u == successfulUser), eq(cronTime));
  }
}
