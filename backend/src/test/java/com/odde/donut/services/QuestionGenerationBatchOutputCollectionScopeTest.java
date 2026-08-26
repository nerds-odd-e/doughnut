package com.odde.donut.services;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;
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
class QuestionGenerationBatchOutputCollectionScopeTest {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchOutputCollectionService outputCollectionService;
  @Autowired QuestionGenerationBatchRepository batchRepository;

  User user;
  Timestamp currentTime;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();
  }

  @Test
  void doesNotCollectAlreadyCollectedBatches() {
    QuestionGenerationBatch batch = saveCompletedBatch();
    batch.setOutputCollectedAt(currentTime);
    batchRepository.saveAndFlush(batch);

    outputCollectionService.collectOutputForCompletedBatches(currentTime);

    verify(openAiApiHandler, never()).retrieveBatch(anyString());
  }

  @Test
  void doesNotCollectNonCompletedBatches() {
    QuestionGenerationBatch batch = saveCompletedBatch();
    batch.setStatus(QuestionGenerationBatchStatus.SUBMITTED);
    batchRepository.saveAndFlush(batch);

    outputCollectionService.collectOutputForCompletedBatches(currentTime);

    verify(openAiApiHandler, never()).retrieveBatch(eq("batch-openai-1"));
  }

  private QuestionGenerationBatch saveCompletedBatch() {
    QuestionGenerationBatch batch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.COMPLETED)
            .plannedAt(currentTime)
            .openaiBatchId("batch-openai-1")
            .please();
    makeMe.entityPersister.flush();
    return batch;
  }
}
