package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteViewer;

public class NoteRealmWithPosition {
  public NotePositionViewedByUser notePosition;
  public NoteRealm noteRealm;

  public static NoteRealmWithPosition fromNote(Note note, User user) {
    NoteRealmWithPosition noteRealmWithPosition = new NoteRealmWithPosition();

    noteRealmWithPosition.notePosition = new NoteViewer(user, note).jsonNotePosition();
    noteRealmWithPosition.noteRealm = new NoteViewer(user, note).toJsonObject();
    return noteRealmWithPosition;
  }
}
