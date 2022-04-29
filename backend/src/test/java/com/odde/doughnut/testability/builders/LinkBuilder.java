package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class LinkBuilder extends EntityBuilder<Link> {
  public LinkBuilder(MakeMe makeMe) {
    super(makeMe, new Link());
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    createdAt(timestamp);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public LinkBuilder between(Note from, Note to, Link.LinkType linkType) {
    entity.setTargetNote(to);
    entity.setSourceNote(from);
    entity.setLinkType(linkType);
    entity.setUser(from.getUser());
    from.getLinks().add(entity);
    to.getRefers().add(entity);
    return this;
  }

  public LinkBuilder between(Note from, Note to) {
    return between(from, to, Link.LinkType.SPECIALIZE);
  }

  public LinkBuilder createdAt(Timestamp timestamp) {
    entity.setCreatedAt(timestamp);

    return this;
  }
}
