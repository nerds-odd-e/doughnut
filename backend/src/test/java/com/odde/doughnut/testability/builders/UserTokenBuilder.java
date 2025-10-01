package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class UserTokenBuilder extends EntityBuilder<UserToken> {
  public UserTokenBuilder(MakeMe makeMe, UserToken userToken) {
    super(makeMe, userToken);
  }

  @Override
  public void beforeCreate(boolean needPersist) {}
}
