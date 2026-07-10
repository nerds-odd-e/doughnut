package com.odde.doughnut.services;

import static com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionTestSupport.completedOpenAiBatch;
import static com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionTestSupport.successLine;
import static org.hamcrest.MatcherAssert.assertThat;
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
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();
    MemoryTracker secondTracker =
        makeMe
            .aMemoryTrackerFor(secondNote)
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();

    completedBatch = new QuestionGenerationBatch();
    completedBatch.setUser(user);
    completedBatch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
    completedBatch.setPlannedAt(currentTime);
    completedBatch.setOpenaiBatchId("batch-openai-1");
    completedBatch = batchRepository.saveAndFlush(completedBatch);

    firstRequest = saveBatchRequest(completedBatch, firstTracker);
    secondRequest = saveBatchRequest(completedBatch, secondTracker);
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

  private QuestionGenerationBatchRequest saveBatchRequest(
      QuestionGenerationBatch batch, MemoryTracker tracker) {
    QuestionGenerationBatchRequest request = new QuestionGenerationBatchRequest();
    request.setBatch(batch);
    request.setMemoryTracker(tracker);
    request.setContextSeed(42L);
    request.setCustomId(QuestionGenerationBatchRequest.customIdFor(batch.getId(), tracker.getId()));
    return batchRequestRepository.saveAndFlush(request);
  }
}
