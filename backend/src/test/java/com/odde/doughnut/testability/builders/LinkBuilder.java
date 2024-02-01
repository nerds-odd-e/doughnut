package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class LinkBuilder extends EntityBuilder<Note> {
  User creator = null;

  public LinkBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) return;
    if (creator == null) {
      if (entity.getParent() != null) {
        this.creator = entity.getParent().getThing().getCreator();
      }
    }
    entity.getThing().setCreator(this.creator);
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    if (entity == null) return;
    if (needPersist) {
      if (entity.getParent() != null) {
        makeMe.refresh(entity.getParent());
      }
      if (entity.getTargetNote() != null) {
        makeMe.refresh(entity.getTargetNote());
      }
    }
  }

  public LinkBuilder creator(User user) {
    this.creator = user;
    return this;
  }

  public LinkBuilder between(Note from, Note to, LinkType linkType) {
    this.entity =
        ModelFactoryService.buildALink(
            from, to, null, linkType, new Timestamp(System.currentTimeMillis()));
    creator(from.getThing().getCreator());
    return this;
  }

  public LinkBuilder between(Note from, Note to) {
    return between(from, to, LinkType.SPECIALIZE);
  }
}
