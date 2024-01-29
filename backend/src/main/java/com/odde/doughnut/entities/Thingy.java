package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Thingy extends EntityIdentifiedByIdOnly {

  abstract void setThing(Thing thing);

  @JsonIgnore
  public abstract Thing getThing();

  @JsonIgnore
  public String getNoteTopic() {
    return getThing().getNote().getTopic();
  }

  @JsonIgnore
  public boolean targetVisibleAsSourceOrTo(User viewer) {
    Thing thing = getThing();
    if (thing.getParentNote().getNotebook() == thing.getTargetNote().getNotebook()) return true;
    if (viewer == null) return false;

    return viewer.canReferTo(thing.getTargetNote().getNotebook());
  }

  @JsonIgnore
  public Link.LinkType getNoteLinkType() {
    return getThing().getLinkType();
  }
}
