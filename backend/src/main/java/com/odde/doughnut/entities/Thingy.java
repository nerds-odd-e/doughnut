package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Thingy extends EntityIdentifiedByIdOnly {

  abstract void setThing(Thing thing);

  @JsonIgnore
  public abstract Thing getThing();
}
