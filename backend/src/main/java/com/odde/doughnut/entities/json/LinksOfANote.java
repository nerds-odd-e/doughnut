package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.models.NoteViewer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Getter;

public class LinksOfANote {
  @Getter private Map<Link.LinkType, LinkViewed> links;

  public static Map<LinkType, LinkViewed> getLinksOfANote(NoteViewer noteViewer) {
    return noteViewer.getAllLinks();
  }

  public static Map<LinkType, LinkViewed> getOpenLinksOfANote(NoteViewer noteViewer) {
    Map<LinkType, LinkViewed> links = getLinksOfANote(noteViewer);
    return links.entrySet().stream()
        .filter(x -> LinkType.openTypes().anyMatch((y) -> x.getKey().equals(y)))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }
}
