package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

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
