package com.odde.donut.testability.builders;

import com.odde.donut.entities.NotebookGroup;
import com.odde.donut.entities.Ownership;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;

public class NotebookGroupBuilder extends EntityBuilder<NotebookGroup> {
  static final TestObjectCounter nameCounter = new TestObjectCounter(n -> "notebookGroup" + n);

  public NotebookGroupBuilder(MakeMe makeMe, NotebookGroup group) {
    super(makeMe, group == null ? new NotebookGroup() : group);
    if (entity.getName() == null) {
      entity.setName(nameCounter.generate());
    }
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public NotebookGroupBuilder ownership(Ownership ownership) {
    entity.setOwnership(ownership);
    return this;
  }

  public NotebookGroupBuilder name(String name) {
    entity.setName(name);
    return this;
  }
}
