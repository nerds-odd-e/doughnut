package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class CommentBuilder extends EntityBuilder<Comment> {
  public CommentBuilder(MakeMe makeMe) {
    super(makeMe, new Comment());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
