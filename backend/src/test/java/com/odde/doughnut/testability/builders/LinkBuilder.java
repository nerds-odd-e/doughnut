package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class LinkBuilder extends EntityBuilder<Thing> {
  User creator = null;

  public LinkBuilder(MakeMe makeMe) {
    super(makeMe, null);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) return;
    if (creator == null) {
      if (entity.getSourceNote() != null) {
        this.creator = entity.getSourceNote().getThing().getCreator();
      }
    }
    entity.setCreator(this.creator);
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    if (entity == null) return;
    if (needPersist) {
      if (entity.getSourceNote() != null) {
        makeMe.refresh(entity.getSourceNote());
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

  public LinkBuilder between(Note from, Note to, Link.LinkType linkType) {
    this.entity =
        ModelFactoryService.buildALink(
                from, to, null, linkType, new Timestamp(System.currentTimeMillis()))
            .getThing();
    creator(from.getThing().getCreator());
    return this;
  }

  public LinkBuilder between(Note from, Note to) {
    return between(from, to, Link.LinkType.SPECIALIZE);
  }
}
