package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class UserBuilder extends EntityBuilder<User> {
  static final TestObjectCounter nameCounter = new TestObjectCounter(n -> "user" + n);

  public UserBuilder(MakeMe makeMe) {
    super(makeMe, new User());
    setInfo(nameCounter.generate());
  }

  public UserBuilder(MakeMe makeMe, String userName) {
    super(makeMe, new User());
    setInfo(userName);
  }

  public UserBuilder(MakeMe makeMe, User user) {
    super(makeMe, user);
  }

  private void setInfo(String userName) {
    entity.setExternalIdentifier(userName);
    entity.setName(userName);
  }

  public UserBuilder withSpaceIntervals(String spaceIntervals) {
    entity.setSpaceIntervals(spaceIntervals);
    return this;
  }

  public UserBuilder withToken(String token) {
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    java.time.LocalDateTime expiry = now.plusYears(1);
    entity.getTokens().add(new com.odde.doughnut.entities.UserToken(entity, token, now, expiry));
    return this;
  }

  public UserModel toModelPlease() {
    return makeMe.modelFactoryService.toUserModel(please());
  }

  @Override
  public void beforeCreate(boolean needPersist) {}

  public UserBuilder dailyAssimilationCount(int i) {
    entity.setDailyAssimilationCount(i);
    return this;
  }
}
