package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class SessionItemBuilder extends EntityBuilder<SessionItem> {

  public SessionItemBuilder(MakeMe makeMe) {
    super(makeMe, new SessionItem());
  }

  public SessionItemBuilder learningSession(LearningSession learningSession) {
    entity.setLearningSession(learningSession);
    return this;
  }

  public SessionItemBuilder memoryTracker(MemoryTracker memoryTracker) {
    entity.setMemoryTracker(memoryTracker);
    return this;
  }

  public SessionItemBuilder noteTitle(String noteTitle) {
    entity.setNoteTitle(noteTitle);
    return this;
  }

  public SessionItemBuilder feedbackScore(Integer feedbackScore) {
    entity.setFeedbackScore(feedbackScore);
    return this;
  }

  public SessionItemBuilder feedbackRecordedAt(Timestamp feedbackRecordedAt) {
    entity.setFeedbackRecordedAt(feedbackRecordedAt);
    return this;
  }

  public SessionItemBuilder preSessionForgettingCurveIndex(Float index) {
    entity.setPreSessionForgettingCurveIndex(index);
    return this;
  }

  public SessionItemBuilder preSessionRecallCount(Integer recallCount) {
    entity.setPreSessionRecallCount(recallCount);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getLearningSession() == null) {
      throw new IllegalStateException("call learningSession() before please()");
    }
    if (entity.getMemoryTracker() == null) {
      throw new IllegalStateException("call memoryTracker() before please()");
    }
    if (entity.getNoteTitle() == null || entity.getNoteTitle().isBlank()) {
      entity.setNoteTitle(entity.getMemoryTracker().getNote().getTitle());
    }
  }
}
