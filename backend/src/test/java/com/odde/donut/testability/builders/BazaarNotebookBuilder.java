package com.odde.donut.testability.builders;

import com.odde.donut.entities.BazaarNotebook;
import com.odde.donut.entities.Notebook;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;

public class BazaarNotebookBuilder extends EntityBuilder<BazaarNotebook> {

  public BazaarNotebookBuilder(MakeMe makeMe, Notebook notebook) {
    super(makeMe, new BazaarNotebook());
    entity.setNotebook(notebook);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
