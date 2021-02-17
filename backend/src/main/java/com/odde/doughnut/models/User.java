package com.odde.doughnut.models;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "user")
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
  @Getter @Setter private String name;
  @Column(name="external_identifier")
  @Getter @Setter private String externalIdentifier;



  @OneToMany(mappedBy="user")
  @Getter @Setter private List<Note> notes = new ArrayList<>();



  public List<Note> getNotesInDescendingOrder() {
    List<Note> notes = getNotes();
    notes.sort(Comparator.comparing(Note::getCreatedDatetime).reversed());
    return notes;
  }
}
