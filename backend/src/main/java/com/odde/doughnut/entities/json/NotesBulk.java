package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.List;

public class NotesBulk {
  public NotePositionViewedByUser notePosition;
  public List<NoteRealm> notes = new ArrayList<>();

  public static NotesBulk jsonNoteRealm(Note note, UserModel user) {
    NotesBulk notesBulk = new NotesBulk();

    notesBulk.notePosition = new NoteViewer(user.getEntity(), note).jsonNotePosition(note);
    notesBulk.notes.add(new NoteViewer(user.getEntity(), note).toJsonObject());
    return notesBulk;
  }

  public static NotesBulk jsonNoteWithDescendants(Note note, UserModel user) {
    NotesBulk notesBulk = new NotesBulk();
    notesBulk.notePosition = new NoteViewer(user.getEntity(), note).jsonNotePosition(note);
    notesBulk.notes.add(new NoteViewer(user.getEntity(), note).toJsonObject());
    note.getDescendantsInBreathFirstOrder()
        .forEach(n -> notesBulk.notes.add(new NoteViewer(user.getEntity(), n).toJsonObject()));
    return notesBulk;
  }
}
