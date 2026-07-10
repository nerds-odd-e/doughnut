package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.QuestionGenerationBatchUserSubmissionTx.DueUserSubmissionOutcome;
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
  void continuesAfterPerUserFailureAndReturnsSummaryCounts() {
    User successfulUser = new User();
    User failingUser = new User();
    when(planningService.findUsersEligibleForBatchSubmission(cronTime))
        .thenReturn(List.of(successfulUser, failingUser));
    when(userSubmissionTx.processDueUser(argThat(u -> u == successfulUser), eq(cronTime)))
        .thenReturn(DueUserSubmissionOutcome.submitted(1, 10, "batch-ok"));
    when(userSubmissionTx.processDueUser(argThat(u -> u == failingUser), eq(cronTime)))
        .thenReturn(DueUserSubmissionOutcome.failed(2, 11));

    var summary = service.submitDueUsers(cronTime);

    assertThat(summary.getConsideredUserCount(), equalTo(2));
    assertThat(summary.getSubmittedCount(), equalTo(1));
    assertThat(summary.getFailedCount(), equalTo(1));
    assertThat(summary.getSkippedCount(), equalTo(0));

    verify(userSubmissionTx).processDueUser(argThat(u -> u == successfulUser), eq(cronTime));
    verify(userSubmissionTx).processDueUser(argThat(u -> u == failingUser), eq(cronTime));
  }

  @Test
  void continuesAfterUnexpectedPerUserException() {
    User firstUser = new User();
    User secondUser = new User();
    when(planningService.findUsersEligibleForBatchSubmission(cronTime))
        .thenReturn(List.of(firstUser, secondUser));
    when(userSubmissionTx.processDueUser(argThat(u -> u == firstUser), eq(cronTime)))
        .thenThrow(new RuntimeException("unexpected"));
    when(userSubmissionTx.processDueUser(argThat(u -> u == secondUser), eq(cronTime)))
        .thenReturn(DueUserSubmissionOutcome.submitted(2, 20, "batch-2"));

    var summary = service.submitDueUsers(cronTime);

    assertThat(summary.getConsideredUserCount(), equalTo(2));
    assertThat(summary.getSubmittedCount(), equalTo(1));
    assertThat(summary.getFailedCount(), equalTo(1));
    assertThat(summary.getSkippedCount(), equalTo(0));
  }
}
