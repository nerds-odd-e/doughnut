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
import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.Optional;
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
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchUserStateRepository userStateRepository;
  @Autowired GlobalSettingsService globalSettingsService;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch plannedBatch;

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

    plannedBatch = planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
  }

  @Nested
  class AcceptedSubmission {
    @Test
    void updatesLocalBatchAndSubmissionMarker() {
      when(openAiApiHandler.uploadBatchInputFile(any())).thenReturn("file-abc");
      when(openAiApiHandler.createResponsesBatch("file-abc")).thenReturn("batch-xyz");

      boolean submitted = submissionService.submitPlannedBatch(plannedBatch, currentTime);

      assertThat(submitted, is(true));

      QuestionGenerationBatch batch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.SUBMITTED));
      assertThat(batch.getOpenaiInputFileId(), equalTo("file-abc"));
      assertThat(batch.getOpenaiBatchId(), equalTo("batch-xyz"));
      assertThat(batch.getSubmittedAt(), equalTo(currentTime));

      QuestionGenerationBatchUserState state =
          userStateRepository.findByUser_Id(user.getId()).orElseThrow();
      assertThat(state.getLastSuccessfulSubmittedAt(), equalTo(currentTime));
    }

    @Test
    void updatesExistingSubmissionMarker() {
      Timestamp previousSubmission =
          new Timestamp(currentTime.getTime() - TimeUnit.DAYS.toMillis(2));
      QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
      state.setUser(user);
      state.setLastSuccessfulSubmittedAt(previousSubmission);
      userStateRepository.save(state);
      makeMe.entityPersister.flush();

      when(openAiApiHandler.uploadBatchInputFile(any())).thenReturn("file-abc");
      when(openAiApiHandler.createResponsesBatch("file-abc")).thenReturn("batch-xyz");

      submissionService.submitPlannedBatch(plannedBatch, currentTime);

      QuestionGenerationBatchUserState updated =
          userStateRepository.findByUser_Id(user.getId()).orElseThrow();
      assertThat(updated.getLastSuccessfulSubmittedAt(), equalTo(currentTime));
    }
  }

  @Nested
  class FailedSubmission {
    Timestamp previousSubmission;

    @BeforeEach
    void setupPreviousMarker() {
      previousSubmission = new Timestamp(currentTime.getTime() - TimeUnit.DAYS.toMillis(2));
      QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
      state.setUser(user);
      state.setLastSuccessfulSubmittedAt(previousSubmission);
      userStateRepository.save(state);
      makeMe.entityPersister.flush();
    }

    @Test
    void uploadFailureMarksBatchFailedWithoutUpdatingMarker() {
      when(openAiApiHandler.uploadBatchInputFile(any()))
          .thenThrow(new RuntimeException("upload failed"));

      boolean submitted = submissionService.submitPlannedBatch(plannedBatch, currentTime);

      assertThat(submitted, is(false));

      QuestionGenerationBatch batch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
      assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.FAILED));
      assertThat(batch.getOpenaiInputFileId(), is(nullValue()));
      assertThat(batch.getOpenaiBatchId(), is(nullValue()));
      assertThat(batch.getSubmittedAt(), is(nullValue()));

      QuestionGenerationBatchUserState state =
          userStateRepository.findByUser_Id(user.getId()).orElseThrow();
      assertThat(state.getLastSuccessfulSubmittedAt(), equalTo(previousSubmission));
    }

    @Test
    void batchCreationFailureMarksBatchFailedWithoutUpdatingMarker() {
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

      QuestionGenerationBatchUserState state =
          userStateRepository.findByUser_Id(user.getId()).orElseThrow();
      assertThat(state.getLastSuccessfulSubmittedAt(), equalTo(previousSubmission));
    }

    @Test
    void leavesMarkerUnsetWhenNoPriorSuccessfulSubmissionExists() {
      userStateRepository.deleteAll();
      makeMe.entityPersister.flush();

      when(openAiApiHandler.uploadBatchInputFile(any()))
          .thenThrow(new RuntimeException("upload failed"));

      submissionService.submitPlannedBatch(plannedBatch, currentTime);

      Optional<QuestionGenerationBatchUserState> state =
          userStateRepository.findByUser_Id(user.getId());
      assertThat(state.isPresent(), is(false));
    }
  }
}
