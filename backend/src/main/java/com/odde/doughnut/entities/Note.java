package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.models.NoteViewer;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "note")
public abstract class Note extends NoteBase {
  public static final int MAX_TITLE_LENGTH = 150;

  public Note buildChildNote(User user, Timestamp currentUTCTimestamp, String topicConstructor) {
    Note note = HierarchicalNote.createNote(user, currentUTCTimestamp, topicConstructor);
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

  @JsonIgnore
  public List<Note> getLinkedSiblingsOfSameLinkType(User user) {
    return getSiblingLinksOfSameLinkType(user).map(Note::getParent).toList();
  }

  public Thing buildNoteThing() {
    Thing result = new Thing();
    result.setNote(this);
    return result;
  }
}
