package com.odde.donut.testability.builders;

import com.odde.donut.entities.Circle;
import com.odde.donut.entities.User;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;

public class CircleBuilder extends EntityBuilder<Circle> {
  static final TestObjectCounter nameCounter = new TestObjectCounter(n -> "circle" + n);

  public CircleBuilder(Circle circle, MakeMe makeMe) {
    super(makeMe, circle == null ? new Circle() : circle);
    entity.setName(nameCounter.generate());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public CircleBuilder hasMember(User user) {
    entity.getMembers().add(user);
    return this;
  }
}
