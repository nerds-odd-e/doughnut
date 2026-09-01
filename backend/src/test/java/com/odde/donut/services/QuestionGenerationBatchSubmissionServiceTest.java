package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.odde.donut.testability.MakeMe;
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
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
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

      submissionService.submitPlannedBatch(plannedBatch, currentTime);

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
}
