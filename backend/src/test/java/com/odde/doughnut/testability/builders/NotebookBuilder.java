package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class NotebookBuilder extends EntityBuilder<Notebook> {
  private BookBuilder bookAttachment;

  public NotebookBuilder(Notebook notebook, MakeMe makeMe) {
    super(makeMe, notebook != null ? notebook : new Notebook());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
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
    if (entity.getCreator() == null) {
      entity.setCreator(makeMe.aUser().please(needPersist));
    }
    if (entity.getOwnership() == null) {
      entity.setOwnership(makeMe.aUser().please(needPersist).getOwnership());
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

  public NotebookBuilder creatorAndOwner(User user) {
    entity.setOwnership(user.getOwnership());
    entity.setCreator(user);
    return this;
  }

  public NotebookBuilder name(String name) {
    entity.setName(name);
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

  public NotebookBuilder readmeContent(String content) {
    entity.setReadmeContent(content);
    return this;
  }
}
