package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.util.function.Consumer;

public class NotebookBuilder extends EntityBuilder<Notebook> {
  private NoteBuilder noteBuilder;
  private final boolean existingNotebook;
  private BookBuilder bookAttachment;

  public NotebookBuilder(Notebook notebook, MakeMe makeMe) {
    super(makeMe, notebook);
    this.existingNotebook = notebook != null;
    if (existingNotebook) {
      this.noteBuilder = null;
    } else {
      this.noteBuilder = makeMe.aNote().attachToNewNotebook(null);
    }
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (noteBuilder != null) {
      Note note = noteBuilder.please(needPersist);
      this.entity = note.getNotebook();
    }
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
    if (noteBuilder != null) {
      noteBuilder.creatorAndOwner(user);
    } else {
      entity.setOwnership(user.getOwnership());
      entity.setCreatorEntity(user);
    }
    return this;
  }

  public NotebookBuilder withNChildrenThat(int count, Consumer<NoteBuilder> childNoteThat) {
    if (noteBuilder == null) {
      noteBuilder = makeMe.aNote().inNotebook(entity);
      if (entity.getCreatorEntity() != null) {
        noteBuilder.creator(entity.getCreatorEntity());
      }
    }
    noteBuilder.withNChildrenThat(count, childNoteThat);
    return this;
  }

  public NotebookBuilder owner(User user) {
    if (noteBuilder != null) {
      noteBuilder.ownership(user.getOwnership());
    } else {
      entity.setOwnership(user.getOwnership());
    }

    return this;
  }

  public NotebookBuilder owner(Circle circle) {
    if (noteBuilder != null) {
      noteBuilder.ownership(circle.getOwnership());
    } else {
      entity.setOwnership(circle.getOwnership());
    }
    return this;
  }
}
