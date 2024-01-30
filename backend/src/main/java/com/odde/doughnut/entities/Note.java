package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.*;

@Entity
@Table(name = "note")
public class Note extends NoteBase {
  public static final int MAX_TITLE_LENGTH = 150;

  private Note() {}

  public static Note createNote(User user, Timestamp currentUTCTimestamp, String topicConstructor) {
    final Note note = new Note();
    note.setUpdatedAt(currentUTCTimestamp);
    note.setTopicConstructor(topicConstructor);
    note.setUpdatedAt(currentUTCTimestamp);

    Thing.createThing(user, note, currentUTCTimestamp);
    return note;
  }

  public Note buildChildNote(User user, Timestamp currentUTCTimestamp, String topicConstructor) {
    Note note = createNote(user, currentUTCTimestamp, topicConstructor);
    note.setParentNote(this);
    return note;
  }

  public void buildNotebookForHeadNote(Ownership ownership, User creator) {
    final Notebook notebook = new Notebook();
    notebook.setCreatorEntity(creator);
    notebook.setOwnership(ownership);
    notebook.setHeadNote(this);
    setNotebook(notebook);
  }
}
