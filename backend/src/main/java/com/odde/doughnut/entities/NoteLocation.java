package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "location")
public class NoteLocation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  private Integer id;

  @Column(name = "latitude")
  @Getter
  @Setter
  private Double latitude;

  @Column(name = "longitude")
  @Getter
  @Setter
  private Double longitude;
}
