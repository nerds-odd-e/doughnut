package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class BazaarNotebookBuilder extends EntityBuilder<BazaarNotebook> {

  public BazaarNotebookBuilder(MakeMe makeMe, Notebook notebook) {
    super(makeMe, new BazaarNotebook());
    entity.setNotebook(notebook);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
