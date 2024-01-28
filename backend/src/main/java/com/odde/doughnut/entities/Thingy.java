package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Thingy extends EntityIdentifiedByIdOnly {

  abstract void setThing(Thing thing);

  @JsonIgnore
  public abstract Thing getThing();

  public String getTopic() {
    return getThing().getNote().getTopic();
  }
}
