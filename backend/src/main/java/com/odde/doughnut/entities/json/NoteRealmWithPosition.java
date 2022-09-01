package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;

public class NoteRealmWithPosition {
  public NotePositionViewedByUser notePosition;
  public NoteRealm noteRealm;

  public static NoteRealmWithPosition fromNote(Note note, UserModel user) {
    NoteRealmWithPosition noteRealmWithPosition = new NoteRealmWithPosition();

    noteRealmWithPosition.notePosition = new NoteViewer(user.getEntity(), note).jsonNotePosition();
    noteRealmWithPosition.noteRealm = new NoteViewer(user.getEntity(), note).toJsonObject();
    return noteRealmWithPosition;
  }
}
