package com.odde.doughnut.services;

import static com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionTestSupport.completedOpenAiBatch;
import static com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionTestSupport.successLine;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.MemoryTracker;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchOutputCollectionDirectBatchTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchOutputCollectionService outputCollectionService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;

  User user;
  Timestamp currentTime;
  QuestionGenerationBatch completedBatch;
  QuestionGenerationBatchRequest firstRequest;
  QuestionGenerationBatchRequest secondRequest;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();

    Note firstNote = makeMe.aNote().notebookOwnedBy(user).please();
    Note secondNote = makeMe.aNote().notebookOwnedBy(user).please();
    MemoryTracker firstTracker =
        makeMe
            .aMemoryTrackerFor(firstNote)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();
    MemoryTracker secondTracker =
        makeMe
            .aMemoryTrackerFor(secondNote)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();

    completedBatch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.COMPLETED)
            .plannedAt(currentTime)
            .openaiBatchId("batch-openai-1")
            .please();
    makeMe.entityPersister.flush();

    firstRequest =
        makeMe
            .aQuestionGenerationBatchRequest()
            .batch(completedBatch)
            .memoryTracker(firstTracker)
            .please();
    secondRequest =
        makeMe
            .aQuestionGenerationBatchRequest()
            .batch(completedBatch)
            .memoryTracker(secondTracker)
            .please();
    makeMe.entityPersister.flush();
  }

  @Test
  void marksMissingOutputLinesAsFailed() {
    when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
    when(openAiApiHandler.downloadFileContent("file-output"))
        .thenReturn(successLine(firstRequest.getCustomId()));
    when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");

    outputCollectionService.collectOutputForCompletedBatches(currentTime);

    QuestionGenerationBatchRequest reloadedSecond =
        batchRequestRepository.findById(secondRequest.getId()).orElseThrow();
    assertThat(reloadedSecond.getStatus(), is(QuestionGenerationBatchRequestStatus.FAILED));
    assertThat(reloadedSecond.getErrorDetail(), is("missing batch output line"));
  }

  @Test
  void ignoresMalformedOutputLinesWithoutFailingOtherRows() {
    when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
    when(openAiApiHandler.downloadFileContent("file-output"))
        .thenReturn(
            "not-json\n"
                + successLine(firstRequest.getCustomId())
                + "\n"
                + "{\"response\":{\"status_code\":200}}");
    when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");

    outputCollectionService.collectOutputForCompletedBatches(currentTime);

    List<QuestionGenerationBatchRequestStatus> statuses =
        batchRequestRepository.findByBatch_Id(completedBatch.getId()).stream()
            .map(QuestionGenerationBatchRequest::getStatus)
            .toList();
    assertThat(
        statuses,
        containsInAnyOrder(
            QuestionGenerationBatchRequestStatus.OUTPUT_READY,
            QuestionGenerationBatchRequestStatus.FAILED));
  }

  @Test
  void ignoresErrorLinesWithMissingCustomId() {
    when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
    when(openAiApiHandler.downloadFileContent("file-output")).thenReturn("");
    when(openAiApiHandler.downloadFileContent("file-error"))
        .thenReturn("{\"error\":{\"message\":\"batch failed\"}}");

    outputCollectionService.collectOutputForCompletedBatches(currentTime);

    List<QuestionGenerationBatchRequestStatus> statuses =
        batchRequestRepository.findByBatch_Id(completedBatch.getId()).stream()
            .map(QuestionGenerationBatchRequest::getStatus)
            .toList();
    assertThat(
        statuses,
        containsInAnyOrder(
            QuestionGenerationBatchRequestStatus.FAILED,
            QuestionGenerationBatchRequestStatus.FAILED));
  }

  @Test
  void downloadsFromPersistedFileIdsWithoutRetrieveBatch() {
    completedBatch.setOpenaiOutputFileId("file-output");
    completedBatch.setOpenaiErrorFileId("file-error");
    batchRepository.saveAndFlush(completedBatch);

    when(openAiApiHandler.downloadFileContent("file-output"))
        .thenReturn(
            successLine(secondRequest.getCustomId())
                + "\n"
                + successLine(firstRequest.getCustomId()));
    when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");

    outputCollectionService.collectOutputForCompletedBatches(currentTime);

    verify(openAiApiHandler, never()).retrieveBatch(anyString());
  }
}
