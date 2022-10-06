package com.odde.doughnut.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoteLocation {

  @Column(name = "latitude")
  public Double latitude;

  @Column(name = "longitude")
  public Double longitude;
}
