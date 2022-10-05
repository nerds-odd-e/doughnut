package com.odde.doughnut.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Embeddable
public class NoteLocation {

  @Column(name = "latitude")
  @Getter
  @Setter
  @Nullable
  public Double latitude;

  @Column(name = "longitude")
  @Getter
  @Setter
  @Nullable
  public Double longitude;
}
