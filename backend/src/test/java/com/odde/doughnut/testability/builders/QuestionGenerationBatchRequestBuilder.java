package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class QuestionGenerationBatchRequestBuilder
    extends EntityBuilder<QuestionGenerationBatchRequest> {

  public QuestionGenerationBatchRequestBuilder(MakeMe makeMe) {
    super(makeMe, new QuestionGenerationBatchRequest());
    entity.setContextSeed(42L);
  }

  public QuestionGenerationBatchRequestBuilder batch(QuestionGenerationBatch batch) {
    entity.setBatch(batch);
    return this;
  }

  public QuestionGenerationBatchRequestBuilder memoryTracker(MemoryTracker memoryTracker) {
    entity.setMemoryTracker(memoryTracker);
    return this;
  }

  public QuestionGenerationBatchRequestBuilder contextSeed(long contextSeed) {
    entity.setContextSeed(contextSeed);
    return this;
  }

  public QuestionGenerationBatchRequestBuilder status(QuestionGenerationBatchRequestStatus status) {
    entity.setStatus(status);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getBatch() == null) {
      throw new IllegalStateException("call batch() before please()");
    }
    if (entity.getMemoryTracker() == null) {
      throw new IllegalStateException("call memoryTracker() before please()");
    }
    if (entity.getCustomId() == null) {
      entity.setCustomId(
          QuestionGenerationBatchRequest.customIdFor(
              entity.getBatch().getId(), entity.getMemoryTracker().getId()));
    }
  }
}
