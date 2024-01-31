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
  private NoteBase note;
  private JsonViewer jsonViewer;

  public NoteViewer(User viewer, Note note) {
    this.viewer = viewer;
    this.note = note;
    this.jsonViewer = new JsonViewer(viewer);
  }

  public NoteRealm toJsonObject() {
    NoteRealm nvb = new NoteRealm();
    nvb.setId(note.getId());
    nvb.setLinks(getAllLinks());
    nvb.setChildren(note.getChildren());
    nvb.setNote(note.getThing().getNote());
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
                        setDirect(linksOfTypeThroughDirect(List.of(type)).stream().toList());
                        setReverse(linksOfTypeThroughReverse(type).toList());
                      }
                    }))
        .filter(x -> x.getValue().notEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public List<Thing> linksOfTypeThroughDirect(List<LinkType> linkTypes) {
    return note.getLinkChildren().stream()
        .filter(l -> l.targetVisibleAsSourceOrTo(viewer))
        .filter(l -> linkTypes.contains(l.getNoteLinkType()))
        .map(Thingy::getThing)
        .toList();
  }

  public Stream<Thing> linksOfTypeThroughReverse(LinkType linkType) {
    return note.getRefers().stream()
        .filter(l -> l.getThing().getLinkType().equals(linkType))
        .filter(l -> l.getThing().sourceVisibleAsTargetOrTo(viewer))
        .map(Thingy::getThing);
  }

  public NotePositionViewedByUser jsonNotePosition() {
    NotePositionViewedByUser nvb = new NotePositionViewedByUser();
    nvb.setNoteId(note.getId());
    nvb.setNotebook(jsonViewer.jsonNotebookViewedByUser(note.getNotebook()));
    nvb.setAncestors(note.getAncestors());
    return nvb;
  }
}
