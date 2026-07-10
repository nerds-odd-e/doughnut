package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import io.micrometer.core.instrument.MeterRegistry;
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
class QuestionGenerationBatchSubmissionServiceTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired MeterRegistry meterRegistry;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired GlobalSettingsService globalSettingsService;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch plannedBatch;
  private double submittedBaseline;
  private double failedBaseline;

  @BeforeEach
  void setup() {
    submittedBaseline = counter("question_generation_batch.submitted");
    failedBaseline = counter("question_generation_batch.failed");
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

    plannedBatch = planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
  }

  @Nested
  class AcceptedSubmission {
    @Test
    void updatesLocalBatchWithSubmittedAt() {
      when(openAiApiHandler.uploadBatchInputFile(any())).thenReturn("file-abc");
      when(openAiApiHandler.createResponsesBatch("file-abc")).thenReturn("batch-xyz");

      boolean submitted = submissionService.submitPlannedBatch(plannedBatch, currentTime);

      assertThat(submitted, is(true));

      QuestionGenerationBatch batch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.SUBMITTED));
      assertThat(batch.getOpenaiInputFileId(), equalTo("file-abc"));
      assertThat(batch.getOpenaiBatchId(), equalTo("batch-xyz"));
      assertThat(batch.getSubmittedAt(), equalTo(currentTime));
      assertThat(
          batchRepository.findLatestSubmittedAtByUser_Id(user.getId()).orElseThrow(),
          equalTo(currentTime));
    }
  }

  @Nested
  class FirstTimeFailedSubmission {
    @Test
    void batchCreationFailureLeavesNoLatestSubmittedAt() {
      when(openAiApiHandler.uploadBatchInputFile(any())).thenReturn("file-abc");
      when(openAiApiHandler.createResponsesBatch("file-abc"))
          .thenThrow(new RuntimeException("batch create failed"));

      boolean submitted = submissionService.submitPlannedBatch(plannedBatch, currentTime);

      assertThat(submitted, is(false));

      QuestionGenerationBatch batch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.FAILED));
      assertThat(batch.getSubmittedAt(), is(nullValue()));
      assertThat(
          batchRepository.findLatestSubmittedAtByUser_Id(user.getId()).isPresent(), is(false));
    }
  }

  @Nested
  class FailedSubmission {
    Timestamp previousSubmission;

    @BeforeEach
    void setupPreviousSubmission() {
      previousSubmission = new Timestamp(currentTime.getTime() - TimeUnit.DAYS.toMillis(2));
      QuestionGenerationBatch priorBatch = new QuestionGenerationBatch();
      priorBatch.setUser(user);
      priorBatch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
      priorBatch.setPlannedAt(previousSubmission);
      priorBatch.setSubmittedAt(previousSubmission);
      batchRepository.save(priorBatch);
      makeMe.entityPersister.flush();
    }

    @Test
    void uploadFailureMarksBatchFailedWithoutUpdatingLatestSubmittedAt() {
      when(openAiApiHandler.uploadBatchInputFile(any()))
          .thenThrow(new RuntimeException("upload failed"));

      boolean submitted = submissionService.submitPlannedBatch(plannedBatch, currentTime);

      assertThat(submitted, is(false));
      assertThat(counter("question_generation_batch.failed") - failedBaseline, is(1.0));
      assertThat(counter("question_generation_batch.submitted") - submittedBaseline, is(0.0));

      QuestionGenerationBatch batch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.FAILED));
      assertThat(batch.getOpenaiInputFileId(), is(nullValue()));
      assertThat(batch.getOpenaiBatchId(), is(nullValue()));
      assertThat(batch.getSubmittedAt(), is(nullValue()));

      assertThat(
          batchRepository.findLatestSubmittedAtByUser_Id(user.getId()).orElseThrow(),
          equalTo(previousSubmission));
    }

    @Test
    void batchCreationFailureMarksBatchFailedWithoutUpdatingLatestSubmittedAt() {
      when(openAiApiHandler.uploadBatchInputFile(any())).thenReturn("file-abc");
      when(openAiApiHandler.createResponsesBatch("file-abc"))
          .thenThrow(new RuntimeException("batch create failed"));

      boolean submitted = submissionService.submitPlannedBatch(plannedBatch, currentTime);

      assertThat(submitted, is(false));

      QuestionGenerationBatch batch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.FAILED));
      assertThat(batch.getOpenaiInputFileId(), is(nullValue()));
      assertThat(batch.getOpenaiBatchId(), is(nullValue()));
      assertThat(batch.getSubmittedAt(), is(nullValue()));

      assertThat(
          batchRepository.findLatestSubmittedAtByUser_Id(user.getId()).orElseThrow(),
          equalTo(previousSubmission));
    }
  }

  private double counter(String name) {
    return meterRegistry.get(name).counter().count();
  }
}
