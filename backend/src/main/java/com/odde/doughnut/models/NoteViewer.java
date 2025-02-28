package com.odde.doughnut.models;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.*;

public class NoteViewer {

  private User viewer;
  private Note note;

  public NoteViewer(User viewer, Note note) {
    this.viewer = viewer;
    this.note = note;
  }

  public NoteRealm toJsonObject() {
    NoteRealm nvb = new NoteRealm(note);
    nvb.setInboundReferences(note.getInboundReferences().stream().filter(this::allowed).toList());
    nvb.setFromBazaar(viewer == null || !viewer.owns(note.getNotebook()));

    return nvb;
  }

  private boolean allowed(Note l) {
    if (l.getParent().getNotebook() == l.getTargetNote().getNotebook()) return true;
    if (viewer == null) return false;
    return viewer.canReferTo(l.getParent().getNotebook());
  }
}
