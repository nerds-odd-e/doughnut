package com.odde.donut.services;

import static com.odde.donut.services.QuestionGenerationBatchPollingTestSupport.openAiBatchWithStatus;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.testability.OpenAiBatchApiMock;
import com.odde.donut.testability.SpringTestBase;
import com.openai.models.batches.Batch;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

class QuestionGenerationBatchPollingScopeTest extends SpringTestBase {

  OpenAiBatchApiMock openAiBatches;

  @Autowired QuestionGenerationBatchPollingService pollingService;

  User user;
  Timestamp currentTime;

  @BeforeEach
  void setup() {
    openAiBatches = new OpenAiBatchApiMock(officialClient);
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();
  }

  @ParameterizedTest
  @EnumSource(
      value = QuestionGenerationBatchStatus.class,
      names = {"COMPLETED", "FAILED", "EXPIRED"})
  void terminalBatchIsNotPolledAgain(QuestionGenerationBatchStatus terminalStatus) {
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .status(terminalStatus)
        .plannedAt(currentTime)
        .openaiBatchId("batch-openai-1")
        .please();
    makeMe.entityPersister.flush();

    pollingService.pollSubmittedBatches();

    verify(openAiBatches.batches(), never()).retrieve(anyString());
  }

  @Test
  void onlyPollsSubmittedBatchesAmongMixedStatuses() {
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .submittedInFlight(currentTime)
        .openaiBatchId("batch-openai-1")
        .please();
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .status(QuestionGenerationBatchStatus.COMPLETED)
        .plannedAt(currentTime)
        .openaiBatchId("batch-completed")
        .please();
    makeMe.entityPersister.flush();

    openAiBatches.stubRetrieve(openAiBatchWithStatus(Batch.Status.IN_PROGRESS));

    pollingService.pollSubmittedBatches();

    verify(openAiBatches.batches()).retrieve("batch-openai-1");
    verify(openAiBatches.batches(), never()).retrieve(eq("batch-completed"));
  }
}
