package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.models.NoteViewer;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.stream.Stream;

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

  public NoteViewer targetNoteViewer(User user) {
    return new NoteViewer(user, getTargetNote());
  }

  @JsonIgnore
  public Stream<Note> getSiblingLinksOfSameLinkType(User user) {
    return targetNoteViewer(user)
        .linksOfTypeThroughReverse(getLinkType())
        .filter(l -> !l.equals(this));
  }
}
