package com.odde.donut.services;

import com.odde.donut.algorithms.PortablePath;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PortablePathAuthoring {

  private final WikiLinkResolver wikiLinkResolver;
  private final AuthorizationService authorizationService;

  public PortablePathAuthoring(
      WikiLinkResolver wikiLinkResolver, AuthorizationService authorizationService) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.authorizationService = authorizationService;
  }

  public String authoredPortablePath(
      Note sourceNote, Note destinationNote, String originalPortablePath) {
    String notePortion =
        shortestUnambiguousNotePortion(destinationNote, authorizationService.getCurrentUser());
    Optional<String> encodedPropertyKey =
        (originalPortablePath == null || originalPortablePath.isBlank())
            ? Optional.empty()
            : PortablePath.parse(originalPortablePath).encodedPropertyKey();
    PortablePath authored = new PortablePath(Optional.empty(), notePortion, encodedPropertyKey);
    if (!sourceNote.getNotebook().getId().equals(destinationNote.getNotebook().getId())) {
      authored = authored.withNotebookName(destinationNote.getNotebook().getName());
    }
    return authored.format();
  }

  public String qualifiedPortablePath(Note destinationNote, User viewer) {
    return WikiLinkMarkdown.escape(
        new PortablePath(
                Optional.of(destinationNote.getNotebook().getName()),
                shortestUnambiguousNotePortion(destinationNote, viewer),
                Optional.empty())
            .format());
  }

  private String shortestUnambiguousNotePortion(Note destinationNote, User viewer) {
    if (displayNameUniquelyIdentifies(destinationNote, viewer)) {
      return destinationNote.getTitle();
    }
    return lengthenedNotePortion(destinationNote);
  }

  private boolean displayNameUniquelyIdentifies(Note destinationNote, User viewer) {
    return wikiLinkResolver.readableNotebookMatchUniquelyIdentifies(
        destinationNote.getNotebook().getName(),
        destinationNote.getTitle(),
        viewer,
        destinationNote);
  }

  private static String lengthenedNotePortion(Note note) {
    List<String> folders = note.folderNamesFromRoot();
    String title = note.getTitle();
    if (folders.isEmpty()) {
      return "/" + title;
    }
    return String.join("/", folders) + "/" + title;
  }
}
