package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class LinkBuilder extends EntityBuilder<Link> {
  public LinkBuilder(MakeMe makeMe) {
    super(
        makeMe, Link.createLink(null, null, null, null, new Timestamp(System.currentTimeMillis())));
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public LinkBuilder setUser(User user) {
    entity.setUser(user);
    entity.getThing().setUser(user);
    return this;
  }

  public LinkBuilder between(Note from, Note to, Link.LinkType linkType) {
    entity.setTargetNote(to);
    entity.setSourceNote(from);
    entity.setLinkType(linkType);
    setUser(from.getUser());
    from.getLinks().add(entity);
    to.getRefers().add(entity);
    return this;
  }

  public LinkBuilder between(Note from, Note to) {
    return between(from, to, Link.LinkType.SPECIALIZE);
  }
}
