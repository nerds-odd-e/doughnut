package com.odde.doughnut.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class NoteLocation {

  @Column(name = "latitude")
  public Double latitude;

  @Column(name = "longitude")
  public Double longitude;
}
