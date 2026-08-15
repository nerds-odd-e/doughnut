package com.odde.doughnut.services;

import static com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionTestSupport.completedOpenAiBatch;
import static com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionTestSupport.successLine;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
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
class QuestionGenerationBatchMaintenanceServiceTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchMaintenanceService maintenanceService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired RecallPromptRepository recallPromptRepository;

  Timestamp currentTime;
  QuestionGenerationBatch submittedBatch;
  QuestionGenerationBatchRequest request;

  @BeforeEach
  void setup() {
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

    GeneratedMcq generatedMcq =
        makeMe
            .aGeneratedMcq()
            .stem("What color is the sky on a clear day?")
            .choices("Blue", "Green", "Red")
            .correctAnswerIndex(0)
            .please();

    when(openAiApiHandler.retrieveBatch("batch-openai-1")).thenReturn(completedOpenAiBatch());
    when(openAiApiHandler.downloadFileContent("file-output"))
        .thenReturn(successLine(request.getCustomId()));
    when(openAiApiHandler.downloadFileContent("file-error")).thenReturn("");
    when(openAiApiHandler.parseStructuredOutputFromBatchSuccessLine(anyString(), any(Class.class)))
        .thenReturn(Optional.of(generatedMcq));
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
