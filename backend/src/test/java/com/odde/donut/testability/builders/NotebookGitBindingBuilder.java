package com.odde.donut.testability.builders;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;

/** A notebook's Git binding row with the minimum every column requires; no accepted history. */
public class NotebookGitBindingBuilder extends EntityBuilder<NotebookGitBinding> {

  public NotebookGitBindingBuilder(MakeMe makeMe, Notebook notebook) {
    super(makeMe, new NotebookGitBinding());
    entity.setNotebook(notebook);
    entity.setAcceptedGitObjectId("a".repeat(40));
    Timestamp now = new Timestamp(System.currentTimeMillis());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
