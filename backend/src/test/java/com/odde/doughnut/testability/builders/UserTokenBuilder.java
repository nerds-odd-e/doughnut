package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.util.UUID;

public class UserTokenBuilder extends EntityBuilder<UserToken> {

  public UserTokenBuilder(MakeMe makeMe) {
    super(makeMe, new UserToken());
    this.entity.setToken(UUID.randomUUID().toString());
  }

  public UserTokenBuilder forUser(com.odde.doughnut.entities.User user) {
    this.entity.setUserId(user.getId());
    return this;
  }

  public UserTokenBuilder withLabel(String label) {
    this.entity.setLabel(label);
    return this;
  }

  @Override
  public void beforeCreate(boolean needPersist) {}
}
