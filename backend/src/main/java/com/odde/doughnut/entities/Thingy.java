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
    if (getThing().getParentNote().getNotebook() == getThing().getTargetNote().getNotebook())
      return true;
    if (viewer == null) return false;

    return viewer.canReferTo(getThing().getTargetNote().getNotebook());
  }

  @JsonIgnore
  public Link.LinkType getNoteLinkType() {
    if (getThing().getLink() != null) {
      return getThing().getLink().getLinkType();
    }
    return getThing().getNote().getLinkType();
  }
}
