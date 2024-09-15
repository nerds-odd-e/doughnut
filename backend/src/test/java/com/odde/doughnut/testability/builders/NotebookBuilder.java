package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class NotebookBuilder extends EntityBuilder<Notebook> {
  private final NoteBuilder noteBuilder;

  public NotebookBuilder(MakeMe makeMe) {
    super(makeMe, null);
    noteBuilder = makeMe.aNote();
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    Note note = noteBuilder.please(needPersist);
    this.entity = note.getNotebook();
  }

  public NotebookBuilder creatorAndOwner(UserModel user) {
    noteBuilder.creatorAndOwner(user);
    return this;
  }
}
