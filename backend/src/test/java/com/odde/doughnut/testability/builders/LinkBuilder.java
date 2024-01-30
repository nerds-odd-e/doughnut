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
  public LinkBuilder(MakeMe makeMe) {
    super(
        makeMe,
        ModelFactoryService.buildALink(
                null, null, null, Link.LinkType.NO_LINK, new Timestamp(System.currentTimeMillis()))
            .getThing());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity == null) return;
    if (needPersist) {
      if (entity.getSourceNote() != null) {
        if (entity.getCreator() == null) {
          entity.setCreator(entity.getSourceNote().getThing().getCreator());
        }
      }
    }
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
    entity.setCreator(user);
    return this;
  }

  public LinkBuilder between(Note from, Note to, Link.LinkType linkType) {
    entity.setTargetNote(to);
    entity.setSourceNote(from);
    entity.setLinkType(linkType);
    creator(from.getThing().getCreator());
    return this;
  }

  public LinkBuilder between(Note from, Note to) {
    return between(from, to, Link.LinkType.SPECIALIZE);
  }
}
