package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.ProductOutcome;
import com.odde.doughnut.entities.RecallLog;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class RecallLogBuilder extends EntityBuilder<RecallLog> {

  public RecallLogBuilder(MakeMe makeMe) {
    super(makeMe, new RecallLog());
  }

  public RecallLogBuilder memoryTracker(MemoryTracker memoryTracker) {
    entity.setMemoryTracker(memoryTracker);
    return this;
  }

  public RecallLogBuilder recordedAt(Timestamp recordedAt) {
    entity.setRecordedAt(recordedAt);
    return this;
  }

  public RecallLogBuilder elapsedHours(Integer elapsedHours) {
    entity.setElapsedHours(elapsedHours);
    return this;
  }

  public RecallLogBuilder productOutcome(ProductOutcome productOutcome) {
    entity.setProductOutcome(productOutcome);
    return this;
  }

  public RecallLogBuilder answer(Answer answer) {
    entity.setAnswer(answer);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getMemoryTracker() == null) {
      throw new IllegalStateException("call memoryTracker() before please()");
    }
    if (entity.getRecordedAt() == null) {
      entity.setRecordedAt(makeMe.testabilitySettings.getCurrentUTCTimestamp());
    }
    if (entity.getProductOutcome() == null) {
      entity.setProductOutcome(ProductOutcome.GOOD);
    }
  }
}
