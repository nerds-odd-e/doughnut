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

  //  @Getter @Setter private List<Link> directHierarchyLinks;

  public static LinksOfANote getLinksOfANote(NoteViewer noteViewer) {
    LinksOfANote links = new LinksOfANote();
    links.links = noteViewer.getAllLinks();
    return links;
  }

  public static LinksOfANote getOpenLinksOfANote(NoteViewer noteViewer) {
    LinksOfANote links = getLinksOfANote(noteViewer);
    links.links =
        links.links.entrySet().stream()
            .filter(x -> LinkType.openTypes().anyMatch((y) -> x.getKey().equals(y)))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    return links;
  }
}
