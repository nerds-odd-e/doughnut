package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class LearningSessionBuilder extends EntityBuilder<LearningSession> {

  private final List<MemoryTracker> sessionItemTrackers = new ArrayList<>();

  public LearningSessionBuilder(MakeMe makeMe) {
    super(makeMe, new LearningSession());
  }

  public LearningSessionBuilder forNotebook(Notebook notebook) {
    entity.setNotebook(notebook);
    return this;
  }

  public LearningSessionBuilder by(User user) {
    entity.setUser(user);
    return this;
  }

  public LearningSessionBuilder status(LearningSessionStatus status) {
    entity.setStatus(status);
    return this;
  }

  public LearningSessionBuilder recordedAt(Timestamp recordedAt) {
    entity.setRecordedAt(recordedAt);
    return this;
  }

  public LearningSessionBuilder withSessionItems(MemoryTracker... trackers) {
    sessionItemTrackers.addAll(List.of(trackers));
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getUser() == null) {
      throw new IllegalStateException("call by() before please()");
    }
    if (entity.getNotebook() == null) {
      throw new IllegalStateException("call forNotebook() before please()");
    }
    if (entity.getStatus() == null) {
      entity.setStatus(LearningSessionStatus.AWAITING_REPORT);
    }
    if (entity.getCommissionedAt() == null) {
      entity.setCommissionedAt(makeMe.aTimestamp().please());
    }
    if (entity.getStatus() == LearningSessionStatus.RECORDED && entity.getRecordedAt() == null) {
      entity.setRecordedAt(entity.getCommissionedAt());
    }
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    Timestamp recordedAt =
        entity.getRecordedAt() != null ? entity.getRecordedAt() : entity.getCommissionedAt();
    for (MemoryTracker tracker : sessionItemTrackers) {
      makeMe
          .aSessionItem()
          .learningSession(entity)
          .memoryTracker(tracker)
          .noteTitle(tracker.getNote().getTitle())
          .feedbackScore(3)
          .feedbackRecordedAt(recordedAt)
          .please(needPersist);
    }
  }
}
