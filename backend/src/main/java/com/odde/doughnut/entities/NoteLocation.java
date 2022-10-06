package com.odde.doughnut.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
public class NoteLocation {

  @Column(name = "latitude")
  @Getter
  @Setter
  public Double latitude;

  @Column(name = "longitude")
  @Getter
  @Setter
  public Double longitude;
}
