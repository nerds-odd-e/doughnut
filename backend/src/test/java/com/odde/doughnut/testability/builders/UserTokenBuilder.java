package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.util.UUID;

public class UserTokenBuilder extends EntityBuilder<UserToken> {

  public UserTokenBuilder(MakeMe makeMe) {
    super(makeMe, new UserToken());
    this.entity.setToken(UUID.randomUUID().toString());
  }

  public UserTokenBuilder forUser(UserModel userModel) {
    this.entity.setUserId(userModel.getEntity().getId());
    return this;
  }

  public UserTokenBuilder withLabel(String label) {
    this.entity.setLabel(label);
    return this;
  }

  @Override
  public void beforeCreate(boolean needPersist) {}
}
