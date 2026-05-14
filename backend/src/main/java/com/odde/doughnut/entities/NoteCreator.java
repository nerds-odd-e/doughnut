package com.odde.doughnut.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "note_creator",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_note_creator_note_user",
            columnNames = {"note_id", "user_id"}))
public class NoteCreator {

  @Id
  @Column(name = "note_id")
  @Getter
  private Integer id;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "note_id")
  @Getter
  @Setter
  private Note note;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
  @Getter
  @Setter
  private User user;

  public static NoteCreator forNoteAndUser(Note note, User user) {
    NoteCreator row = new NoteCreator();
    row.setNote(note);
    row.setUser(user);
    return row;
  }
}
