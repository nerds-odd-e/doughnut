package com.odde.donut.services;

import static com.odde.donut.services.QuestionGenerationBatchImportPayloadSupport.batchSuccessLine;
import static com.odde.donut.services.QuestionGenerationBatchOutputCollectionTestSupport.stubCompletedOpenAiBatch;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.testability.OpenAiBatchApiMock;
import com.odde.donut.testability.SpringTestBase;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class QuestionGenerationBatchMaintenanceServiceTest extends SpringTestBase {

  OpenAiBatchApiMock openAiBatches;

  @Autowired QuestionGenerationBatchMaintenanceService maintenanceService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired RecallPromptRepository recallPromptRepository;

  Timestamp currentTime;
  QuestionGenerationBatch submittedBatch;
  QuestionGenerationBatchRequest request;

  @BeforeEach
  void setup() throws JsonProcessingException {
    openAiBatches = new OpenAiBatchApiMock(officialClient);
    User user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();

    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    MemoryTracker memoryTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();

    submittedBatch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .submittedInFlight(currentTime)
            .submittedAt(currentTime)
            .openaiBatchId("batch-openai-1")
            .please();
    makeMe.entityPersister.flush();

    request =
        makeMe
            .aQuestionGenerationBatchRequest()
            .batch(submittedBatch)
            .memoryTracker(memoryTracker)
            .please();
    makeMe.entityPersister.flush();

    GeneratedMcq generatedMcq = makeMe.aGeneratedMcq().please();

    stubCompletedOpenAiBatch(
        openAiBatches, batchSuccessLine(request.getCustomId(), generatedMcq), "");
  }

  @Test
  void resumesPollingOutputCollectionAndImportFromPersistedState() {
    maintenanceService.resumeExistingBatches(currentTime);

    QuestionGenerationBatch reloadedBatch =
        batchRepository.findById(submittedBatch.getId()).orElseThrow();
    assertThat(reloadedBatch.getStatus(), is(QuestionGenerationBatchStatus.COMPLETED));
    assertThat(reloadedBatch.getOutputCollectedAt(), is(currentTime));
    assertThat(reloadedBatch.getImportedAt(), is(currentTime));

    QuestionGenerationBatchRequest reloadedRequest =
        batchRequestRepository.findById(request.getId()).orElseThrow();
    assertThat(reloadedRequest.getStatus(), is(QuestionGenerationBatchRequestStatus.IMPORTED));

    List<RecallPrompt> recallPrompts =
        recallPromptRepository.findAllByMemoryTracker_IdOrderByIdDesc(
            request.getMemoryTracker().getId());
    assertThat(recallPrompts.size(), is(1));
  }
}
