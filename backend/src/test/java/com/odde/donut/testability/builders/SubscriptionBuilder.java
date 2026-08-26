package com.odde.donut.testability.builders;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.Subscription;
import com.odde.donut.entities.User;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;

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

  public SubscriptionBuilder daily(int dailyTargetOfNewNotes) {
    entity.setDailyTargetOfNewNotes(dailyTargetOfNewNotes);
    return this;
  }
}
