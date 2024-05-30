package com.odde.doughnut.models;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.*;
import java.util.List;
import java.util.stream.Stream;

public class NoteViewer {

  private User viewer;
  private Note note;

  public NoteViewer(User viewer, Note note) {
    this.viewer = viewer;
    this.note = note;
  }

  public NoteRealm toJsonObject() {
    NoteRealm nvb = new NoteRealm(note);
    nvb.setRefers(getRefers());
    nvb.setFromBazaar(viewer == null || !viewer.owns(note.getNotebook()));

    return nvb;
  }

  public List<LinkingNote> getRefers() {
    return note.getRefers().stream().filter(l -> allowed(l)).toList();
  }

  public List<LinkingNote> linksOfTypeThroughDirect(List<LinkType> linkTypes) {
    return note.getLinks().stream()
        .filter(l -> l.targetVisibleAsSourceOrTo(viewer))
        .filter(l -> linkTypes.contains(l.getLinkType()))
        .toList();
  }

  public Stream<LinkingNote> linksOfTypeThroughReverse(LinkType linkType) {
    return note.getRefers().stream()
        .filter(l -> l.getLinkType().equals(linkType))
        .filter(l -> allowed(l));
  }

  private boolean allowed(LinkingNote l) {
    if (l.getParent().getNotebook() == l.getTargetNote().getNotebook()) return true;
    if (viewer == null) return false;
    return viewer.canReferTo(l.getParent().getNotebook());
  }
}
