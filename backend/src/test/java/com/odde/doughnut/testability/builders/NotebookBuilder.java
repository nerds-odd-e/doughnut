package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.function.Consumer;

public class NotebookBuilder extends EntityBuilder<Notebook> {
  private final boolean existingNotebook;
  private BookBuilder bookAttachment;
  private NoteBuilder childTreeBuilder;

  public NotebookBuilder(Notebook notebook, MakeMe makeMe) {
    super(makeMe, notebook != null ? notebook : new Notebook());
    this.existingNotebook = notebook != null;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (existingNotebook) {
      return;
    }
    Timestamp now = new Timestamp(System.currentTimeMillis());
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);
    } else if (entity.getUpdatedAt() == null) {
      entity.setUpdatedAt(entity.getCreatedAt());
    }
    if (entity.getName() == null || entity.getName().isBlank()) {
      entity.setName(NoteBuilder.notebookTestNameCounter.generate());
    }
    if (entity.getCreatorEntity() == null && entity.getOwnership() == null) {
      User user = makeMe.aUser().please(needPersist);
      entity.setCreatorEntity(user);
      entity.setOwnership(user.getOwnership());
    } else if (entity.getOwnership() == null) {
      entity.setOwnership(entity.getCreatorEntity().getOwnership());
    }
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    if (childTreeBuilder != null) {
      childTreeBuilder.please(needPersist);
    }
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
    entity.setOwnership(user.getOwnership());
    entity.setCreatorEntity(user);
    return this;
  }

  public NotebookBuilder name(String name) {
    entity.setName(name);
    return this;
  }

  public NotebookBuilder withNChildrenThat(int count, Consumer<NoteBuilder> childNoteThat) {
    if (childTreeBuilder == null) {
      childTreeBuilder = makeMe.aNote().inNotebook(entity);
      if (entity.getCreatorEntity() != null) {
        childTreeBuilder.creator(entity.getCreatorEntity());
      }
    }
    childTreeBuilder.withNChildrenThat(count, childNoteThat);
    return this;
  }

  public NotebookBuilder owner(User user) {
    entity.setOwnership(user.getOwnership());
    return this;
  }

  public NotebookBuilder owner(Circle circle) {
    entity.setOwnership(circle.getOwnership());
    return this;
  }
}
