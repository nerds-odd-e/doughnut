package com.odde.donut.services;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.testability.OpenAiBatchApiMock;
import com.odde.donut.testability.SpringTestBase;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class QuestionGenerationBatchOutputCollectionScopeTest extends SpringTestBase {

  OpenAiBatchApiMock openAiBatches;

  @Autowired QuestionGenerationBatchOutputCollectionService outputCollectionService;
  @Autowired QuestionGenerationBatchRepository batchRepository;

  User user;
  Timestamp currentTime;

  @BeforeEach
  void setup() {
    openAiBatches = new OpenAiBatchApiMock(officialClient);
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();
  }

  @Test
  void doesNotCollectAlreadyCollectedBatches() {
    QuestionGenerationBatch batch = saveCompletedBatch();
    batch.setOutputCollectedAt(currentTime);
    batchRepository.saveAndFlush(batch);

    outputCollectionService.collectOutputForCompletedBatches(currentTime);

    verify(openAiBatches.batches(), never()).retrieve(anyString());
  }

  @Test
  void doesNotCollectNonCompletedBatches() {
    QuestionGenerationBatch batch = saveCompletedBatch();
    batch.setStatus(QuestionGenerationBatchStatus.SUBMITTED);
    batchRepository.saveAndFlush(batch);

    outputCollectionService.collectOutputForCompletedBatches(currentTime);

    verify(openAiBatches.batches(), never()).retrieve(eq("batch-openai-1"));
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
