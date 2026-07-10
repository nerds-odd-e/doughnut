package com.odde.doughnut.services;

import static com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionTestSupport.completedOpenAiBatch;
import static com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionTestSupport.errorLine;
import static com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionTestSupport.successLine;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
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
class QuestionGenerationBatchOutputCollectionServiceTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchOutputCollectionService outputCollectionService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired GlobalSettingsService globalSettingsService;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch completedBatch;
  QuestionGenerationBatchRequest firstRequest;
  QuestionGenerationBatchRequest secondRequest;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");

    Note firstNote = makeMe.aNote().notebookOwnedBy(user).please();
    Note secondNote = makeMe.aNote().notebookOwnedBy(user).please();
    makeMe
        .aMemoryTrackerFor(firstNote)
        .by(user)
        .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
        .please();
    makeMe
        .aMemoryTrackerFor(secondNote)
        .by(user)
        .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
        .please();

    QuestionGenerationBatch plannedBatch =
        planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
    when(openAiApiHandler.uploadBatchInputFile(org.mockito.ArgumentMatchers.any()))
        .thenReturn("file-abc");
    when(openAiApiHandler.createResponsesBatch("file-abc")).thenReturn("batch-openai-1");
    submissionService.submitPlannedBatch(plannedBatch, currentTime);

    completedBatch = batchRepository.findById(plannedBatch.getId()).orElseThrow();
    completedBatch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
    batchRepository.saveAndFlush(completedBatch);

    List<QuestionGenerationBatchRequest> requests =
        batchRequestRepository.findByBatch_Id(completedBatch.getId());
    firstRequest = requests.get(0);
    secondRequest = requests.get(1);
  }

  @Nested
  class OutputMapping {
    @Test
    void mapsUnorderedOutputLinesToRequestRows() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
      when(openAiApiHandler.downloadFileContent("file-output"))
          .thenReturn(
              successLine(secondRequest.getCustomId())
                  + "\n"
                  + successLine(firstRequest.getCustomId()));
      when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");

      outputCollectionService.collectOutputForCompletedBatches(currentTime);

      QuestionGenerationBatchRequest reloadedFirst =
          batchRequestRepository.findById(firstRequest.getId()).orElseThrow();
      QuestionGenerationBatchRequest reloadedSecond =
          batchRequestRepository.findById(secondRequest.getId()).orElseThrow();

      assertThat(reloadedFirst.getStatus(), is(QuestionGenerationBatchRequestStatus.OUTPUT_READY));
      assertThat(reloadedSecond.getStatus(), is(QuestionGenerationBatchRequestStatus.OUTPUT_READY));
      assertThat(reloadedFirst.getRawSuccessPayload(), is(successLine(firstRequest.getCustomId())));
      assertThat(
          reloadedSecond.getRawSuccessPayload(), is(successLine(secondRequest.getCustomId())));
      assertThat(reloadedFirst.getRawErrorPayload(), is(nullValue()));
      assertThat(reloadedSecond.getRawErrorPayload(), is(nullValue()));

      QuestionGenerationBatch reloadedBatch =
          batchRepository.findById(completedBatch.getId()).orElseThrow();
      assertThat(reloadedBatch.getOpenaiOutputFileId(), is("file-output"));
      assertThat(reloadedBatch.getOpenaiErrorFileId(), is("file-error"));
      assertThat(reloadedBatch.getOutputCollectedAt(), is(currentTime));
    }

    @Test
    void mapsErrorFileRowsToFailedRequests() {
      when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
      when(openAiApiHandler.downloadFileContent("file-output"))
          .thenReturn(successLine(firstRequest.getCustomId()));
      when(openAiApiHandler.downloadFileContent("file-error"))
          .thenReturn(errorLine(secondRequest.getCustomId(), "model unavailable"));

      outputCollectionService.collectOutputForCompletedBatches(currentTime);

      QuestionGenerationBatchRequest reloadedFirst =
          batchRequestRepository.findById(firstRequest.getId()).orElseThrow();
      QuestionGenerationBatchRequest reloadedSecond =
          batchRequestRepository.findById(secondRequest.getId()).orElseThrow();

      assertThat(reloadedFirst.getStatus(), is(QuestionGenerationBatchRequestStatus.OUTPUT_READY));
      assertThat(reloadedSecond.getStatus(), is(QuestionGenerationBatchRequestStatus.FAILED));
      assertThat(
          reloadedSecond.getRawErrorPayload(),
          is(errorLine(secondRequest.getCustomId(), "model unavailable")));
      assertThat(reloadedSecond.getErrorDetail(), is("model unavailable"));
    }
  }
}
