package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteViewer;
import java.util.ArrayList;
import java.util.List;

public class NoteRealmWithAllDescendants {
  public NotePositionViewedByUser notePosition;
  public List<NoteRealm> notes = new ArrayList<>();

  public static NoteRealmWithAllDescendants fromNote(Note note, User user) {
    NoteRealmWithAllDescendants notesBulk = new NoteRealmWithAllDescendants();
    notesBulk.notePosition = new NoteViewer(user, note).jsonNotePosition();
    notesBulk.notes.add(new NoteViewer(user, note).toJsonObject());
    note.getDescendantsInBreathFirstOrder()
        .forEach(n -> notesBulk.notes.add(new NoteViewer(user, n).toJsonObject()));
    return notesBulk;
  }
}
