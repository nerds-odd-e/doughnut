package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.batches.Batch;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchPollingServiceTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchPollingService pollingService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired GlobalSettingsService globalSettingsService;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch submittedBatch;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");

    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    makeMe
        .aMemoryTrackerFor(note)
        .by(user)
        .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
        .please();

    QuestionGenerationBatch plannedBatch =
        planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
    when(openAiApiHandler.uploadBatchInputFile(org.mockito.ArgumentMatchers.any()))
        .thenReturn("file-abc");
    when(openAiApiHandler.createResponsesBatch("file-abc")).thenReturn("batch-openai-1");
    submissionService.submitPlannedBatch(plannedBatch, currentTime);
    submittedBatch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
  }

  private Batch openAiBatchWithStatus(Batch.Status status) {
    return Batch.builder()
        .id("batch-openai-1")
        .completionWindow("24h")
        .createdAt(1L)
        .endpoint("/v1/responses")
        .inputFileId("file-abc")
        .status(status)
        .build();
  }

  @Nested
  class OpenAiStatusUpdates {
    @Test
    void inProgressLeavesBatchSubmitted() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(openAiBatchWithStatus(Batch.Status.IN_PROGRESS));

      pollingService.pollSubmittedBatches();

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.SUBMITTED));
      verify(openAiApiHandler).retrieveBatch("batch-openai-1");
    }

    @Test
    void completedUpdatesLocalBatch() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(openAiBatchWithStatus(Batch.Status.COMPLETED));

      pollingService.pollSubmittedBatches();

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.COMPLETED));
    }

    @Test
    void failedUpdatesLocalBatch() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(openAiBatchWithStatus(Batch.Status.FAILED));

      pollingService.pollSubmittedBatches();

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.FAILED));
    }

    @Test
    void expiredUpdatesLocalBatch() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(openAiBatchWithStatus(Batch.Status.EXPIRED));

      pollingService.pollSubmittedBatches();

      QuestionGenerationBatch batch =
          batchRepository.findById(submittedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.EXPIRED));
    }
  }

  @Nested
  class TerminalBatchesAreNotPolled {
    @Test
    void completedBatchIsNotPolledAgain() {
      submittedBatch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
      batchRepository.saveAndFlush(submittedBatch);

      pollingService.pollSubmittedBatches();

      verify(openAiApiHandler, never()).retrieveBatch(anyString());
    }

    @Test
    void failedBatchIsNotPolledAgain() {
      submittedBatch.setStatus(QuestionGenerationBatchStatus.FAILED);
      batchRepository.saveAndFlush(submittedBatch);

      pollingService.pollSubmittedBatches();

      verify(openAiApiHandler, never()).retrieveBatch(anyString());
    }

    @Test
    void expiredBatchIsNotPolledAgain() {
      submittedBatch.setStatus(QuestionGenerationBatchStatus.EXPIRED);
      batchRepository.saveAndFlush(submittedBatch);

      pollingService.pollSubmittedBatches();

      verify(openAiApiHandler, never()).retrieveBatch(anyString());
    }
  }

  @Nested
  class PollingIsolation {
    @Test
    void onlyPollsSubmittedBatchesAmongMixedStatuses() {
      QuestionGenerationBatch completedBatch = new QuestionGenerationBatch();
      completedBatch.setUser(user);
      completedBatch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
      completedBatch.setPlannedAt(currentTime);
      completedBatch.setOpenaiBatchId("batch-completed");
      batchRepository.saveAndFlush(completedBatch);

      when(openAiApiHandler.retrieveBatch("batch-openai-1"))
          .thenReturn(openAiBatchWithStatus(Batch.Status.IN_PROGRESS));

      pollingService.pollSubmittedBatches();

      verify(openAiApiHandler).retrieveBatch("batch-openai-1");
      verify(openAiApiHandler, never()).retrieveBatch(eq("batch-completed"));
    }
  }
}
