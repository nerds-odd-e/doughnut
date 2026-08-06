package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class QuestionGenerationBatchBuilder extends EntityBuilder<QuestionGenerationBatch> {

  public QuestionGenerationBatchBuilder(MakeMe makeMe) {
    super(makeMe, new QuestionGenerationBatch());
  }

  public QuestionGenerationBatchBuilder forUser(User user) {
    entity.setUser(user);
    return this;
  }

  public QuestionGenerationBatchBuilder status(QuestionGenerationBatchStatus status) {
    entity.setStatus(status);
    return this;
  }

  public QuestionGenerationBatchBuilder plannedAt(Timestamp plannedAt) {
    entity.setPlannedAt(plannedAt);
    return this;
  }

  public QuestionGenerationBatchBuilder submittedAt(Timestamp submittedAt) {
    entity.setSubmittedAt(submittedAt);
    return this;
  }

  public QuestionGenerationBatchBuilder openaiBatchId(String openaiBatchId) {
    entity.setOpenaiBatchId(openaiBatchId);
    return this;
  }

  public QuestionGenerationBatchBuilder completedAt(Timestamp completedAt) {
    return status(QuestionGenerationBatchStatus.COMPLETED)
        .plannedAt(completedAt)
        .submittedAt(completedAt);
  }

  public QuestionGenerationBatchBuilder submittedInFlight(Timestamp plannedAt) {
    return status(QuestionGenerationBatchStatus.SUBMITTED).plannedAt(plannedAt);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getUser() == null) {
      throw new IllegalStateException("call forUser() before please()");
    }
    if (entity.getStatus() == null) {
      entity.setStatus(QuestionGenerationBatchStatus.PLANNED);
    }
    if (entity.getPlannedAt() == null) {
      entity.setPlannedAt(makeMe.aTimestamp().please());
    }
  }
}
