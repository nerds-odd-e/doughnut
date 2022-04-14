package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.util.Optional;

public class CommentBuilder extends EntityBuilder<Comment> {
  public CommentBuilder(MakeMe makeMe) {
    super(makeMe, new Comment());
  }

  public CommentBuilder byNote(Note note) {
    entity.setParentNote(Optional.of(note));
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
  }
}
