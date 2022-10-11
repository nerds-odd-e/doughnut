package com.odde.doughnut.entities;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "location")
public class NoteLocation {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  private Note note;

  @Column(name = "latitude")
  @Getter
  @Setter
  private Double latitude;

  @Column(name = "longitude")
  @Getter
  @Setter
  private Double longitude;

  public static NoteLocation build(Note note, Double latitude, Double longitude) {
    NoteLocation noteLocation = new NoteLocation();
    noteLocation.latitude = latitude;
    noteLocation.longitude = longitude;
    noteLocation.note = note;
    note.setLocation(noteLocation);
    return noteLocation;
  }
}
