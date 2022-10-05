package com.odde.doughnut.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.Data;
import org.springframework.lang.Nullable;

@Embeddable
@Data
public class NoteLocation {

  @Column(name = "latitude")
  @Nullable
  public Double latitude;

  @Column(name = "longitude")
  @Nullable
  public Double longitude;
}
