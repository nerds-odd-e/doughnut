package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchUserScheduleDTO;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestionGenerationBatchUserScheduleNoCandidateTrackersTest {

  QuestionGenerationBatchRepository batchRepository;
  RecallPromptRepository recallPromptRepository;
  MemoryTrackerRepository memoryTrackerRepository;
  QuestionGenerationBatchPlanningService planningService;

  User user;

  @BeforeEach
  void setup() {
    batchRepository = mock(QuestionGenerationBatchRepository.class);
    recallPromptRepository = mock(RecallPromptRepository.class);
    memoryTrackerRepository = mock(MemoryTrackerRepository.class);
    planningService =
        new QuestionGenerationBatchPlanningService(
            batchRepository,
            mock(QuestionGenerationBatchRequestRepository.class),
            recallPromptRepository,
            memoryTrackerRepository,
            mock(UserRepository.class));

    user = new User();
    RecallPrompt answeredRecall = mock(RecallPrompt.class);
    when(answeredRecall.getAnswerTime())
        .thenReturn(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 8, 30)));

    when(batchRepository.existsByUser_IdAndStatus(
            any(), eq(QuestionGenerationBatchStatus.SUBMITTED)))
        .thenReturn(false);
    when(batchRepository.findLatestSubmittedAtByUser_Id(any())).thenReturn(Optional.empty());
    when(recallPromptRepository.findAnsweredRecallPromptsInTimeRange(
            any(), any(Timestamp.class), any(Timestamp.class)))
        .thenReturn(List.of(answeredRecall));
    when(memoryTrackerRepository.findBatchQuestionGenerationCandidatesByUser(
            any(), any(Timestamp.class)))
        .thenReturn(Collections.emptyList());
  }

  @Test
  void returnsNoCandidateTrackersReasonWhenEligibleScheduleHasNoBatchCandidates() {
    Timestamp currentTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 0));

    QuestionGenerationBatchUserScheduleDTO schedule =
        planningService.getNextBatchQuestionSchedule(user, currentTime);

    assertThat(schedule.getNextScheduledAt(), is(nullValue()));
    assertThat(
        schedule.getReason(),
        is(QuestionGenerationBatchPlanningService.REASON_NO_CANDIDATE_TRACKERS));
  }
}
