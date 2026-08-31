package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchQueuedRequestCandidateTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;

  User user;
  Timestamp currentTime;
  MemoryTracker dueTracker;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    dueTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();
  }

  @Test
  void excludesTrackerAlreadyQueuedInLiveBatchBeforePromptIsImported() {
    saveBatchRequest(QuestionGenerationBatchStatus.SUBMITTED);

    assertThat(
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime), empty());
  }

  @Test
  void excludesTrackerWithOutputReadyBatchRequestBeforePromptIsImported() {
    saveBatchRequest(
        QuestionGenerationBatchStatus.COMPLETED, QuestionGenerationBatchRequestStatus.OUTPUT_READY);

    assertThat(
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime), empty());
  }

  @Test
  void excludesTrackerWhileFailedBatchStillHasPendingRequest() {
    saveBatchRequest(QuestionGenerationBatchStatus.FAILED);

    assertThat(
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime), empty());
  }

  @Test
  void includesTrackerOnceAfterPreviousRequestFailed() {
    saveBatchRequest(
        QuestionGenerationBatchStatus.FAILED, QuestionGenerationBatchRequestStatus.FAILED);

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(), contains(dueTracker.getId()));
  }

  @Test
  void excludesTrackerAfterFailedRequestWasAlreadyRetried() {
    saveBatchRequest(
        QuestionGenerationBatchStatus.FAILED, QuestionGenerationBatchRequestStatus.FAILED);
    saveBatchRequest(
        QuestionGenerationBatchStatus.FAILED, QuestionGenerationBatchRequestStatus.FAILED);

    assertThat(
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime), empty());
  }

  @Test
  void includesTrackerAfterImportedRequestFollowedEarlierFailures() {
    saveBatchRequest(
        QuestionGenerationBatchStatus.FAILED, QuestionGenerationBatchRequestStatus.FAILED);
    saveBatchRequest(
        QuestionGenerationBatchStatus.FAILED, QuestionGenerationBatchRequestStatus.FAILED);
    saveBatchRequest(
        QuestionGenerationBatchStatus.COMPLETED, QuestionGenerationBatchRequestStatus.IMPORTED);

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);

    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(), contains(dueTracker.getId()));
  }

  private void saveBatchRequest(QuestionGenerationBatchStatus batchStatus) {
    saveBatchRequest(batchStatus, QuestionGenerationBatchRequestStatus.PENDING);
  }

  private void saveBatchRequest(
      QuestionGenerationBatchStatus batchStatus,
      QuestionGenerationBatchRequestStatus requestStatus) {
    QuestionGenerationBatch batch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(batchStatus)
            .plannedAt(currentTime)
            .please();
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(batch)
        .memoryTracker(dueTracker)
        .status(requestStatus)
        .please();
  }
}
