package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchRetentionServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired QuestionGenerationBatchRetentionService retentionService;

  User user;
  MemoryTracker memoryTracker;
  Timestamp currentTime;
  Duration retentionWindow;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
    currentTime = makeMe.aTimestamp().please();
    retentionWindow = Duration.ofDays(30);
  }

  @Nested
  class OldTerminalBatches {

    @Test
    void removesFailedBatchAndRequestRows() {
      QuestionGenerationBatch batch =
          saveBatch(QuestionGenerationBatchStatus.FAILED, oldTimestamp(), null, "batch-failed");

      QuestionGenerationBatchRequest request = saveRequest(batch);

      retentionService.pruneTerminalBatches(currentTime, retentionWindow);

      assertThat(batchRepository.findById(batch.getId()).orElse(null), is(nullValue()));
      assertThat(batchRequestRepository.findById(request.getId()).orElse(null), is(nullValue()));
    }

    @Test
    void removesExpiredBatchAndRequestRows() {
      QuestionGenerationBatch batch =
          saveBatch(QuestionGenerationBatchStatus.EXPIRED, oldTimestamp(), null, "batch-expired");

      QuestionGenerationBatchRequest request = saveRequest(batch);

      retentionService.pruneTerminalBatches(currentTime, retentionWindow);

      assertThat(batchRepository.findById(batch.getId()).orElse(null), is(nullValue()));
      assertThat(batchRequestRepository.findById(request.getId()).orElse(null), is(nullValue()));
    }

    @Test
    void removesImportedCompletedBatchAndRequestRows() {
      Timestamp oldImportedAt = oldTimestamp();
      QuestionGenerationBatch batch =
          saveBatch(
              QuestionGenerationBatchStatus.COMPLETED,
              oldImportedAt,
              oldImportedAt,
              "batch-completed");

      QuestionGenerationBatchRequest request = saveRequest(batch);

      retentionService.pruneTerminalBatches(currentTime, retentionWindow);

      assertThat(batchRepository.findById(batch.getId()).orElse(null), is(nullValue()));
      assertThat(batchRequestRepository.findById(request.getId()).orElse(null), is(nullValue()));
    }
  }

  @Nested
  class RecentTerminalBatches {

    @Test
    void retainsFailedBatchWithinRetentionWindow() {
      QuestionGenerationBatch batch =
          saveBatch(
              QuestionGenerationBatchStatus.FAILED, recentTimestamp(), null, "batch-recent-failed");

      QuestionGenerationBatchRequest request = saveRequest(batch);

      retentionService.pruneTerminalBatches(currentTime, retentionWindow);

      assertThat(batchRepository.findById(batch.getId()).orElse(null), is(notNullValue()));
      assertThat(batchRequestRepository.findById(request.getId()).orElse(null), is(notNullValue()));
    }

    @Test
    void retainsImportedCompletedBatchWithinRetentionWindow() {
      Timestamp recentImportedAt = recentTimestamp();
      QuestionGenerationBatch batch =
          saveBatch(
              QuestionGenerationBatchStatus.COMPLETED,
              recentImportedAt,
              recentImportedAt,
              "batch-recent-completed");

      QuestionGenerationBatchRequest request = saveRequest(batch);

      retentionService.pruneTerminalBatches(currentTime, retentionWindow);

      assertThat(batchRepository.findById(batch.getId()).orElse(null), is(notNullValue()));
      assertThat(batchRequestRepository.findById(request.getId()).orElse(null), is(notNullValue()));
    }
  }

  @Nested
  class NonTerminalBatches {

    @Test
    void neverRemovesPlannedBatch() {
      QuestionGenerationBatch batch =
          saveBatch(QuestionGenerationBatchStatus.PLANNED, oldTimestamp(), null, null);

      QuestionGenerationBatchRequest request = saveRequest(batch);

      retentionService.pruneTerminalBatches(currentTime, retentionWindow);

      assertThat(batchRepository.findById(batch.getId()).orElse(null), is(notNullValue()));
      assertThat(batchRequestRepository.findById(request.getId()).orElse(null), is(notNullValue()));
    }

    @Test
    void neverRemovesSubmittedBatch() {
      QuestionGenerationBatch batch =
          saveBatch(
              QuestionGenerationBatchStatus.SUBMITTED, oldTimestamp(), null, "batch-submitted");

      QuestionGenerationBatchRequest request = saveRequest(batch);

      retentionService.pruneTerminalBatches(currentTime, retentionWindow);

      assertThat(batchRepository.findById(batch.getId()).orElse(null), is(notNullValue()));
      assertThat(batchRequestRepository.findById(request.getId()).orElse(null), is(notNullValue()));
    }

    @Test
    void neverRemovesCompletedBatchAwaitingImport() {
      QuestionGenerationBatch batch =
          saveBatch(
              QuestionGenerationBatchStatus.COMPLETED,
              oldTimestamp(),
              null,
              "batch-awaiting-import");

      QuestionGenerationBatchRequest request = saveRequest(batch);

      retentionService.pruneTerminalBatches(currentTime, retentionWindow);

      assertThat(batchRepository.findById(batch.getId()).orElse(null), is(notNullValue()));
      assertThat(batchRequestRepository.findById(request.getId()).orElse(null), is(notNullValue()));
    }
  }

  private Timestamp oldTimestamp() {
    return new Timestamp(
        currentTime.getTime() - retentionWindow.toMillis() - TimeUnit.DAYS.toMillis(1));
  }

  private Timestamp recentTimestamp() {
    return new Timestamp(currentTime.getTime() - TimeUnit.DAYS.toMillis(1));
  }

  private QuestionGenerationBatch saveBatch(
      QuestionGenerationBatchStatus status,
      Timestamp plannedAt,
      Timestamp importedAt,
      String openaiBatchId) {
    QuestionGenerationBatch batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(status);
    batch.setPlannedAt(plannedAt);
    batch.setSubmittedAt(plannedAt);
    batch.setImportedAt(importedAt);
    batch.setOpenaiBatchId(openaiBatchId);
    return batchRepository.saveAndFlush(batch);
  }

  private QuestionGenerationBatchRequest saveRequest(QuestionGenerationBatch batch) {
    QuestionGenerationBatchRequest request = new QuestionGenerationBatchRequest();
    request.setBatch(batch);
    request.setMemoryTracker(memoryTracker);
    request.setContextSeed(42L);
    request.setCustomId(
        QuestionGenerationBatchRequest.customIdFor(batch.getId(), memoryTracker.getId()));
    request.setStatus(QuestionGenerationBatchRequestStatus.IMPORTED);
    return batchRequestRepository.saveAndFlush(request);
  }
}
