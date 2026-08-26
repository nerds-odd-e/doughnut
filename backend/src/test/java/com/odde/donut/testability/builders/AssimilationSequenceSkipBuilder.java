package com.odde.donut.testability.builders;

import com.odde.donut.entities.AssimilationSequenceSkip;
import com.odde.donut.entities.User;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;

public class AssimilationSequenceSkipBuilder extends EntityBuilder<AssimilationSequenceSkip> {

  public AssimilationSequenceSkipBuilder(AssimilationSequenceSkip skip, MakeMe makeMe) {
    super(makeMe, skip);
    entity.setPropertyKey("");
    entity.setSkippedAt(makeMe.testabilitySettings.getCurrentUTCTimestamp());
  }

  public AssimilationSequenceSkipBuilder by(User user) {
    entity.setUser(user);
    return this;
  }

  public AssimilationSequenceSkipBuilder propertyKey(String propertyKey) {
    entity.setPropertyKey(propertyKey == null ? "" : propertyKey);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
