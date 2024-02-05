package com.odde.doughnut.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "hierarchical_note")
@PrimaryKeyJoinColumn(name = "note_id")
public class HierarchicalNote extends Note {
  private HierarchicalNote() {}

  public static Note createNote(User user, Timestamp currentUTCTimestamp, String topicConstructor) {
    final Note note = new HierarchicalNote();
    note.setUpdatedAt(currentUTCTimestamp);
    note.setTopicConstructor(topicConstructor);
    note.setCreatedAt(currentUTCTimestamp);
    note.setUpdatedAt(currentUTCTimestamp);
    note.setCreator(user);
    return note;
  }
}
