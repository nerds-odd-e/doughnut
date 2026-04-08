package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.util.function.Consumer;

public class NotebookBuilder extends EntityBuilder<Notebook> {
  private final NoteBuilder noteBuilder;
  private BookBuilder bookAttachment;

  public NotebookBuilder(Notebook notebook, MakeMe makeMe) {
    super(makeMe, notebook);
    if (notebook != null) {
      noteBuilder = makeMe.theNote(notebook.getHeadNote());
    } else {
      noteBuilder = makeMe.aNote().asHeadNoteOfANotebook(null);
    }
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    Note note = noteBuilder.please(needPersist);
    this.entity = note.getNotebook();
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    if (bookAttachment != null) {
      bookAttachment.notebook(entity);
      bookAttachment.please(needPersist);
    }
  }

  public NotebookBuilder withBook(String name) {
    this.bookAttachment = makeMe.aBook().bookName(name);
    return this;
  }

  public NotebookBuilder withABook(BookBuilder bookBuilder) {
    this.bookAttachment = bookBuilder;
    return this;
  }

  public NotebookBuilder creatorAndOwner(User user) {
    noteBuilder.creatorAndOwner(user);
    return this;
  }

  public NotebookBuilder withNChildrenThat(int count, Consumer<NoteBuilder> childNoteThat) {
    noteBuilder.withNChildrenThat(count, childNoteThat);
    return this;
  }

  public NotebookBuilder owner(User user) {
    noteBuilder.ownership(user.getOwnership());

    return this;
  }

  public NotebookBuilder owner(Circle circle) {
    noteBuilder.ownership(circle.getOwnership());
    return this;
  }
}
