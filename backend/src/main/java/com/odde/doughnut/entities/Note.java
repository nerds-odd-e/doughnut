package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "note")
public class Note extends NoteBase {
  public static final int MAX_TITLE_LENGTH = 150;

  private Note() {}

  public static Note createNote(User user, Timestamp currentUTCTimestamp, String topicConstructor) {
    final Note note = new Note();
    note.setUpdatedAt(currentUTCTimestamp);
    note.setTopicConstructor(topicConstructor);
    note.setCreatedAt(currentUTCTimestamp);
    note.setUpdatedAt(currentUTCTimestamp);

    final Thing thing = new Thing();
    thing.setNote(note);
    thing.setCreator(user);
    note.setThing(thing);
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
