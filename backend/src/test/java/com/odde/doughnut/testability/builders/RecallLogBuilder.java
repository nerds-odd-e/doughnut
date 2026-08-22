package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.Grade;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.RecallLog;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class RecallLogBuilder extends EntityBuilder<RecallLog> {
  private boolean outcomeSet;

  public RecallLogBuilder(MakeMe makeMe) {
    super(makeMe, new RecallLog());
    entity.setElapsedHours(0);
  }

  public RecallLogBuilder memoryTracker(MemoryTracker memoryTracker) {
    entity.setMemoryTracker(memoryTracker);
    return this;
  }

  public RecallLogBuilder recordedAt(Timestamp recordedAt) {
    entity.setRecordedAt(recordedAt);
    return this;
  }

  public RecallLogBuilder elapsedHours(int elapsedHours) {
    entity.setElapsedHours(elapsedHours);
    return this;
  }

  public RecallLogBuilder grade(Grade grade) {
    entity.setGrade(grade);
    outcomeSet = true;
    return this;
  }

  public RecallLogBuilder confusion() {
    entity.setConfusion();
    outcomeSet = true;
    return this;
  }

  public RecallLogBuilder answer(Answer answer) {
    entity.setAnswer(answer);
    return this;
  }

  public RecallLogBuilder tutorFeedback(String tutorFeedback) {
    entity.setTutorFeedback(tutorFeedback);
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
    if (!outcomeSet) {
      entity.setGrade(Grade.GOOD);
    }
  }
}
