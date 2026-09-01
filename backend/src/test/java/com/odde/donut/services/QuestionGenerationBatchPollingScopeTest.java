package com.odde.donut.services;

import static com.odde.donut.services.QuestionGenerationBatchPollingTestSupport.openAiBatchWithStatus;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.odde.donut.testability.MakeMe;
import com.openai.models.batches.Batch;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchPollingScopeTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPollingService pollingService;

  User user;
  Timestamp currentTime;

  @BeforeEach
  void setup() {
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

    verify(openAiApiHandler, never()).retrieveBatch(anyString());
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

    when(openAiApiHandler.retrieveBatch("batch-openai-1"))
        .thenReturn(openAiBatchWithStatus(Batch.Status.IN_PROGRESS));

    pollingService.pollSubmittedBatches();

    verify(openAiApiHandler).retrieveBatch("batch-openai-1");
    verify(openAiApiHandler, never()).retrieveBatch(eq("batch-completed"));
  }
}
