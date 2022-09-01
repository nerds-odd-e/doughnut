package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.List;

public class NoteRealmWithAllDescendants {
  public NotePositionViewedByUser notePosition;
  public List<NoteRealm> notes = new ArrayList<>();

  public static NoteRealmWithAllDescendants fromNote(Note note, UserModel user) {
    NoteRealmWithAllDescendants notesBulk = new NoteRealmWithAllDescendants();
    notesBulk.notePosition = new NoteViewer(user.getEntity(), note).jsonNotePosition();
    notesBulk.notes.add(new NoteViewer(user.getEntity(), note).toJsonObject());
    note.getDescendantsInBreathFirstOrder()
        .forEach(n -> notesBulk.notes.add(new NoteViewer(user.getEntity(), n).toJsonObject()));
    return notesBulk;
  }
}
