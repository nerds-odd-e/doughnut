package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class SubscriptionBuilder extends EntityBuilder<Subscription> {
  public SubscriptionBuilder(MakeMe makeMe, Subscription entity) {
    super(makeMe, entity);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public SubscriptionBuilder forNotebook(Notebook notebook) {
    entity.setNotebook(notebook);
    return this;
  }

  public SubscriptionBuilder forUser(User user) {
    entity.setUser(user);
    return this;
  }
}
