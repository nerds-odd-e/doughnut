package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "location")
public class NoteLocation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  private Integer id;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @JsonIgnore
  @Setter
  private Note note;

  @Column(name = "latitude")
  @Getter
  @Setter
  private Double latitude;

  @Column(name = "longitude")
  @Getter
  @Setter
  private Double longitude;
}
