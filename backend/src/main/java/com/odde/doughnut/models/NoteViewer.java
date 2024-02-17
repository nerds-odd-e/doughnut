package com.odde.doughnut.models;

import com.odde.doughnut.controllers.json.LinkViewed;
import com.odde.doughnut.controllers.json.NotePositionViewedByUser;
import com.odde.doughnut.controllers.json.NoteRealm;
import com.odde.doughnut.entities.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    nvb.setLinks(getAllLinks());
    nvb.setNotePosition(jsonNotePosition());

    return nvb;
  }

  public Map<LinkType, LinkViewed> getAllLinks() {
    return Arrays.stream(LinkType.values())
        .map(
            type ->
                Map.entry(
                    type,
                    new LinkViewed() {
                      {
                        setDirect(
                            linksOfTypeThroughDirect(List.of(type)).stream()
                                .map(Note::buildNoteThing)
                                .toList());
                        setReverse(
                            linksOfTypeThroughReverse(type).map(Note::buildNoteThing).toList());
                      }
                    }))
        .filter(x -> x.getValue().notEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
        .filter(
            l -> {
              if (l.getParent().getNotebook() == l.getTargetNote().getNotebook()) return true;
              if (viewer == null) return false;
              return viewer.canReferTo(l.getParent().getNotebook());
            });
  }

  public NotePositionViewedByUser jsonNotePosition() {
    NotePositionViewedByUser nvb = new NotePositionViewedByUser();
    nvb.setNoteId(note.getId());
    nvb.setFromBazaar(viewer == null || !viewer.owns(note.getNotebook()));
    nvb.setCircle(note.getNotebook().getOwnership().getCircle());
    nvb.setAncestors(note.getAncestors());
    return nvb;
  }
}
