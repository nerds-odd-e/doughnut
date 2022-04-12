package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.List;

public class NotesBulk {
  public NotePositionViewedByUser notePosition;
  public List<NoteRealm> notes = new ArrayList<>();

  public static NotesBulk jsonNoteWithChildren(Note note, UserModel user) {
    NotesBulk notesBulk = new NotesBulk();

    notesBulk.notePosition = new NoteViewer(user.getEntity(), note).jsonNotePosition(note);
    notesBulk.notes.add(new NoteViewer(user.getEntity(), note).toJsonObject());
    note.getChildren()
        .forEach(
            n -> {
              notesBulk.notes.add(new NoteViewer(user.getEntity(), n).toJsonObject());
            });
    return notesBulk;
  }

  public static NotesBulk jsonNoteWitheDescendants(Note note, UserModel user) {
    NotesBulk notesBulk = new NotesBulk();
    notesBulk.notePosition = new NoteViewer(user.getEntity(), note).jsonNotePosition(note);
    notesBulk.notes.add(new NoteViewer(user.getEntity(), note).toJsonObject());
    note.getDescendantsInBreathFirstOrder()
        .forEach(n -> notesBulk.notes.add(new NoteViewer(user.getEntity(), n).toJsonObject()));
    return notesBulk;
  }

  public static NotesBulk jsonNoteWithParent(Note note, UserModel user) {
    NotesBulk notesBulk = new NotesBulk();
    notesBulk.notePosition = new NoteViewer(user.getEntity(), note).jsonNotePosition(note);
    if (note.getParentNote() != null) {
      notesBulk.notes.add(new NoteViewer(user.getEntity(), note.getParentNote()).toJsonObject());
    }
    return notesBulk;
  }
}
