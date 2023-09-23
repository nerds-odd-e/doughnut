package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.controllers.json.LinkViewed;
import com.odde.doughnut.controllers.json.NotePositionViewedByUser;
import com.odde.doughnut.controllers.json.NoteRealm;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NoteViewer {

  private User viewer;
  private Note note;
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
    nvb.setNote(note);
    nvb.setNotePosition(jsonNotePosition(false));

    return nvb;
  }

  public Map<Link.LinkType, LinkViewed> getAllLinks() {
    return Arrays.stream(Link.LinkType.values())
        .map(
            type ->
                Map.entry(
                    type,
                    new LinkViewed() {
                      {
                        setDirect(linksOfTypeThroughDirect(List.of(type)));
                        setReverse(linksOfTypeThroughReverse(type).collect(Collectors.toList()));
                      }
                    }))
        .filter(x -> x.getValue().notEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public List<Link> linksOfTypeThroughDirect(List<Link.LinkType> linkTypes) {
    return note.getLinks().stream()
        .filter(l -> l.targetVisibleAsSourceOrTo(viewer))
        .filter(l -> linkTypes.contains(l.getLinkType()))
        .collect(Collectors.toList());
  }

  public Stream<Link> linksOfTypeThroughReverse(Link.LinkType linkType) {
    return note.getRefers().stream()
        .filter(l -> l.getLinkType().equals(linkType))
        .filter(l -> l.sourceVisibleAsTargetOrTo(viewer));
  }

  public NotePositionViewedByUser jsonNotePosition(boolean inclusive) {
    NotePositionViewedByUser nvb = new NotePositionViewedByUser();
    nvb.setNoteId(note.getId());
    nvb.setNotebook(jsonViewer.jsonNotebookViewedByUser(note.getNotebook()));
    nvb.setAncestors(note.getAncestors());
    if (inclusive) {
      nvb.getAncestors().add(note);
    }
    return nvb;
  }

  public NotePositionViewedByUser jsonNotePosition() {
    return jsonNotePosition(false);
  }
}
