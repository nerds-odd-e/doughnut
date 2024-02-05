package com.odde.doughnut.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "linking_note")
@PrimaryKeyJoinColumn(name = "note_id")
public class LinkingNote extends Note {
  private LinkingNote() {}

  public static LinkingNote createLink(
      User user, Note parentNote, Timestamp currentUTCTimestamp, String topicConstructor) {
    final LinkingNote note = new LinkingNote();
    note.initialize(user, parentNote, currentUTCTimestamp, topicConstructor);
    return note;
  }
}
